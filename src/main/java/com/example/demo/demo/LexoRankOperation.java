package com.example.demo.demo;

import com.atlassian.fugue.Maybe;
import com.atlassian.fugue.Option;
import com.atlassian.greenhopper.manager.lexorank.LexoRankDao;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRow;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRowUtils;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRow.RankRowType;
import com.atlassian.greenhopper.manager.lexorank.lock.Lock;
import com.atlassian.greenhopper.manager.lexorank.lock.LockOutcome;
import com.atlassian.greenhopper.manager.lexorank.lock.LockProcessOutcome;
import com.atlassian.greenhopper.model.validation.ErrorCollection.Reason;
import com.atlassian.greenhopper.service.ServiceOutcome;
import com.atlassian.greenhopper.service.lexorank.*;
import com.atlassian.greenhopper.service.lexorank.LexoRankStatisticsAgent.Operation;
import com.example.demo.test.LexoRank;
import com.example.demo.test.LexoRankChange;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * 排序操作
 *
 * @author dinghuang123@gmail.com
 * @since 2018/5/25
 */
public class LexoRankOperation {

    private static final int LEXORANK_MAX_LENGTH = 254;
    private static final Map<LexoRankOperation.RankOperationType, Operation> rankOperationToLexoOperationMap = Maps.newHashMap();
    private final LexoRankDao dao;
    private final LexoRankStatisticsAgent statisticsAgent;
    private final LexoRankOperation.RankOperationType rankOperationType;
    private final Long issueToRankIssueId;
    private final Long issueToRankAroundIssueId;
    private final Long rankFieldId;
    private final Integer remainingRankOperations;
    private static final ConcurrentMap<Long, ReentrantLock> rankFieldIdLocks;

    private LexoRankOperation(LexoRankDao dao, LexoRankStatisticsAgent statisticsAgent, LexoRankOperation.RankOperationType rankOperationType, Long issueToRankIssueId, Long issueToRankAroundIssueId, Long rankFieldId, Integer remainingRankOperations) {
        this.dao = dao;
        this.statisticsAgent = statisticsAgent;
        this.rankOperationType = rankOperationType;
        this.issueToRankIssueId = issueToRankIssueId;
        this.issueToRankAroundIssueId = issueToRankAroundIssueId;
        this.rankFieldId = rankFieldId;
        this.remainingRankOperations = remainingRankOperations;
    }

    public LexoRankOperationOutcome execute() {
        this.logStartOfOperation();
        LexoRankOperationOutcome lexoRankOperationOutcome;
        switch (this.rankOperationType.ordinal()) {
            case 1:
            case 2:
                //排名后的操作
                lexoRankOperationOutcome = this.rankRelativeToOtherIssue();
                break;
            case 3:
            case 4:
                //排名最后
                lexoRankOperationOutcome = this.rankFirstOrLast();
                break;
            case 5:
                //排名最初
                lexoRankOperationOutcome = this.rankInitially();
                break;
            default:
                throw new IllegalArgumentException("Unsupported rank operation type");
        }

        this.logEndOfOperation();
        return lexoRankOperationOutcome;
    }

    private void logStartOfOperation() {
        this.statisticsAgent.startOperation(rankOperationToLexoOperationMap.get(this.rankOperationType));
    }

    private void logEndOfOperation() {
        this.statisticsAgent.endOperation(rankOperationToLexoOperationMap.get(this.rankOperationType));
    }

    private LexoRankOperationOutcome rankInitially() {
        //最初排序
        long timeoutTime = System.currentTimeMillis() + (long) LexoRankSettings.RANK_INITIALLY_TIMEOUT_MS;
        BackoffHandler backoffHandler = new BackoffHandler(this.statisticsAgent, timeoutTime);
        while (true) {
            Option maybeRanked;
            ReentrantLock jvmLock;
            do {
                if (System.currentTimeMillis() >= timeoutTime) {
                    return LexoRankOperationOutcome.timeout();
                }
                maybeRanked = this.dao.findByFieldAndIssueId(this.rankFieldId, this.issueToRankIssueId);
                if (maybeRanked.isDefined()) {
                    LexoRankRow rankRow = (LexoRankRow) maybeRanked.get();
                    LexoRank existingRank = LexoRank.parse(rankRow.getRank());
                    LexoRankChange lexoRankChange = LexoRankChange.builder().forRankField(this.rankFieldId).rankedIssue(this.issueToRankIssueId).noop(existingRank).build();
                    return LexoRankOperationOutcome.ok(lexoRankChange);
                }
                backoffHandler.maybeWait();
                jvmLock = rankFieldIdLocks.computeIfAbsent(this.rankFieldId, id -> new ReentrantLock());
            } while (!jvmLock.tryLock());
            LexoRankOperationOutcome var16;
            try {
                LexoRankRow[] unlockedRankRows = this.dao.getMaximumMarkerRowAndPreviousRow(this.rankFieldId);
                LockOutcome lockOutcome = this.dao.acquireLock(unlockedRankRows);
                Lock lock = lockOutcome.get();
                if (!lockOutcome.isValid()) {
                    if (lock != null) {
                        this.dao.releaseLock(lock);
                    }
                    continue;
                }
                try {
                    maybeRanked = this.dao.findByFieldAndIssueId(this.rankFieldId, this.issueToRankIssueId);
                    Object newRankRow;
                    if (maybeRanked.isDefined()) {
                        LexoRankRow rankRow = (LexoRankRow) maybeRanked.get();
                        LexoRank existingRank = LexoRank.parse(rankRow.getRank());
                        LexoRankChange lexoRankChange = LexoRankChange.builder().forRankField(this.rankFieldId).rankedIssue(this.issueToRankIssueId).noop(existingRank).build();
                        newRankRow = LexoRankOperationOutcome.ok(lexoRankChange);
                        return (LexoRankOperationOutcome) newRankRow;
                    }
                    LexoRankRow[] rankRows = this.dao.getMaximumMarkerRowAndPreviousRow(this.rankFieldId);
                    if (LexoRankRowUtils.areRowsDifferent(unlockedRankRows, rankRows)) {
                        continue;
                    }
                    LexoRankRow maximumMarkerRow = rankRows[0];
                    LexoRankRow previousRankRow = rankRows[1];
                    LexoRank initialRank;
                    LexoRank lastRank;
                    if (previousRankRow.getType().equals(RankRowType.MINIMUM_MARKER_ROW)) {
                        lastRank = LexoRank.parse(maximumMarkerRow.getRank());
                        initialRank = LexoRank.from(lastRank.getBucket(), LexoRank.MID_DECIMAL);
                    } else {
                        lastRank = LexoRank.parse(rankRows[1].getRank());
                        initialRank = lastRank.genNext();
                    }
                    boolean rankExistsForFieldId = this.dao.existsRankForFieldId(this.rankFieldId, initialRank.format());
                    if (rankExistsForFieldId) {
                        continue;
                    }
                    Object lexoRankChange;
                    if (this.exceedsMaxRankLength(initialRank)) {
                        lexoRankChange = LexoRankOperationOutcome.error(Reason.VALIDATION_FAILED, "gh.api.rank.error.lexorank.fieldlength.exceeded.norebalance");
                        return (LexoRankOperationOutcome) lexoRankChange;
                    }
                    newRankRow = this.dao.create(this.rankFieldId, this.issueToRankIssueId, initialRank.format());
                    lexoRankChange = LexoRankChange.builder().forRankField(this.rankFieldId).rankedIssue(this.issueToRankIssueId).initially(LexoRank.parse(((LexoRankRow) newRankRow).getRank())).build();
                    var16 = LexoRankOperationOutcome.ok(lexoRankChange, Lists.newArrayList(this.issueToRankIssueId));
                } finally {
                    this.dao.releaseLock(lock);
                }
            } finally {
                jvmLock.unlock();
            }

            return var16;
        }
    }

    private LexoRankOperationOutcome rankRelativeToOtherIssue() {
        long timeoutTime = System.currentTimeMillis() + (long) LexoRankSettings.RANK_RETRY_TIMEOUT_MS;
        BackoffHandler backoffHandler = new BackoffHandler(this.statisticsAgent, timeoutTime);
        Set<Long> issueIdsToReIndex = Sets.newHashSet();
        //超时4S后不执行
        while (System.currentTimeMillis() < timeoutTime) {
            //线程补偿操作，线程睡眠0S
            backoffHandler.maybeWait();
            //根据fieldId和issueId查询排序表rank对象列表，查询条件type=7
            Maybe<LexoRankRow> maybeUnlockedRankRow = this.dao.findByFieldAndIssueId(this.rankFieldId, this.issueToRankIssueId);
            if (maybeUnlockedRankRow.isEmpty()) {
                //请求重新建立issueIds的索引
                return LexoRankOperationOutcome.reindexRequired();
            }
            LexoRankRow unlockedRankRow = maybeUnlockedRankRow.get();
            if (this.issueToRankIssueId.equals(this.issueToRankAroundIssueId)) {
                LexoRank existingRank = LexoRank.parse(unlockedRankRow.getRank());
                LexoRankChange lexoRankChange = LexoRankChange.builder().forRankField(this.rankFieldId).rankedIssue(this.issueToRankIssueId).noop(existingRank).build();
                return LexoRankOperationOutcome.ok(lexoRankChange, issueIdsToReIndex);
            }
            Maybe<LexoRankRow> maybeUnlockedOtherRankRow = this.dao.findByFieldAndIssueId(this.rankFieldId, this.issueToRankAroundIssueId);
            if (maybeUnlockedOtherRankRow.isEmpty()) {
                return LexoRankOperationOutcome.reindexRequired();
            }
            LexoRankRow unlockedOtherRankRow = maybeUnlockedOtherRankRow.get();
            //根据rankId获取rank和前一个rank
            LexoRankRow[] unlockedLexoRankRows = this.rankOperationType.equals(LexoRankOperation.RankOperationType.RANK_BEFORE) ? this.dao.getRowByRankAndPreviousRow(this.rankFieldId, unlockedOtherRankRow.getRank()) : this.dao.getRowByRankAndNextRow(this.rankFieldId, unlockedOtherRankRow.getRank());
            if (unlockedLexoRankRows.length >= 2) {
                //如果都有，对重复的排名进行修复
                LexoRankHealOperation healOperation = LexoRankHealOperation.builder(this.dao, this.statisticsAgent).forRankField(this.rankFieldId).heal(new LexoRankRow[]{unlockedRankRow, unlockedLexoRankRows[0], unlockedLexoRankRows[1]}).build();
                LexoRankOperationOutcome<Boolean> healOperationOutcome = healOperation.execute();
                if (healOperationOutcome.isValid() && healOperationOutcome.getResult()) {
                    //修复成功加入issueId索引
                    issueIdsToReIndex.addAll(healOperationOutcome.getIssueIdsToReIndex());
                } else if (LexoRankRowUtils.areRowsDifferent(unlockedOtherRankRow, unlockedLexoRankRows[0])) {
                    //不是同一个日志打印错误
                } else {
                    //获取新的排名
                    LexoRank rankBetweenFrom;
                    if (this.issueToRankIssueId.equals(unlockedLexoRankRows[1].getIssueId())) {
                        rankBetweenFrom = LexoRank.parse(unlockedLexoRankRows[1].getRank());
                        LexoRankChange lexoRankChange = LexoRankChange.builder().forRankField(this.rankFieldId).rankedIssue(this.issueToRankIssueId).noop(rankBetweenFrom).build();
                        //返回变化的排名
                        return LexoRankOperationOutcome.ok(lexoRankChange, issueIdsToReIndex);
                    }
                    rankBetweenFrom = LexoRank.parse(unlockedLexoRankRows[0].getRank());
                    LexoRank rankBetweenTo = LexoRank.parse(unlockedLexoRankRows[1].getRank());
                    if (!rankBetweenFrom.getBucket().equals(rankBetweenTo.getBucket())) {
                        //不过不是一个桶内，报告异常信息
                    } else {
                        Set<LexoRankRow> rowsToLock = Sets.newHashSet(unlockedRankRow, unlockedLexoRankRows[0], unlockedLexoRankRows[1]);
                        //获取线程锁（方法执行，只能有rowsToLock.size()数量的线程产生）
                        LockOutcome lockOutcome = this.dao.acquireLock(rowsToLock);
                        Lock lock = lockOutcome.get();
                        if (!lockOutcome.isValid()) {
                            if (lock != null) {
                                //释放锁
                                this.dao.releaseLock(lock);
                            }
                        } else {
                            try {
                                LexoRankRow rankRow = this.dao.getByFieldAndIssueId(this.rankFieldId, this.issueToRankIssueId);
                                if (LexoRankRowUtils.areRowsDifferent(unlockedRankRow, rankRow)) {
                                    //抛出异常信息
                                } else {
                                    //按照rank排序
                                    LexoRankRow[] unlockedLexoRankRowsSorted = Stream.of(unlockedLexoRankRows).sorted(Comparator.comparing(LexoRankRow::getRank)).toArray(LexoRankRow[]::new);
                                    //拿到第一个rank和下面的rank
                                    LexoRankRow[] lockedRankRows = this.dao.getRowByRankAndNextRow(this.rankFieldId, unlockedLexoRankRowsSorted[0].getRank());
                                    if (LexoRankRowUtils.areRowsDifferent(lockedRankRows, unlockedLexoRankRowsSorted)) {
                                        //抛出异常信息
                                    } else {
                                        LexoRankOperationOutcome var22;
                                        try {
                                            LexoRankRow[] prevNeighbours;
                                            Optional outcomeOptional;
                                            if (rankRow.getRank().compareTo(lockedRankRows[0].getRank()) < 0) {
                                                prevNeighbours = this.dao.getRowByRankAndNextRow(this.rankFieldId, rankRow.getRank());
                                                if (prevNeighbours.length > 1 && prevNeighbours[0].equals(rankRow) && prevNeighbours[1].equals(lockedRankRows[0])) {
                                                    //交换排名
                                                    outcomeOptional = this.swapRanks(lock, rankRow, prevNeighbours[1], issueIdsToReIndex);
                                                    if (!outcomeOptional.isPresent()) {
                                                        continue;
                                                    }
                                                    var22 = (LexoRankOperationOutcome) outcomeOptional.get();
                                                    return var22;
                                                }
                                            } else {
                                                prevNeighbours = this.dao.getRowByRankAndNextRow(this.rankFieldId, lockedRankRows[1].getRank());
                                                if (prevNeighbours.length > 1 && prevNeighbours[0].equals(lockedRankRows[1]) && prevNeighbours[1].equals(rankRow)) {
                                                    //交换排名
                                                    outcomeOptional = this.swapRanks(lock, rankRow, prevNeighbours[0], issueIdsToReIndex);
                                                    if (!outcomeOptional.isPresent()) {
                                                        continue;
                                                    }
                                                    var22 = (LexoRankOperationOutcome) outcomeOptional.get();
                                                    return var22;
                                                }
                                            }
                                        } catch (Exception var34) {
                                            LexoRankOperationOutcome var21 = LexoRankOperationOutcome.error(Reason.SERVER_ERROR, "server internal exception", var34.getMessage());
                                            return var21;
                                        }
                                        //根据最高和最低获取相关排名
                                        LexoRank newRank = this.getRankRelativeToOtherIssue(lockedRankRows);
                                        rankRow.setRank(newRank.format());
                                        boolean existsRankForFieldId = this.dao.existsRankForFieldId(this.rankFieldId, newRank.format());
                                        if (existsRankForFieldId) {
                                            var22 = LexoRankOperationOutcome.duplicateRank();
                                            return var22;
                                        }
                                        //超过最大长度
                                        if (this.exceedsMaxRankLength(newRank)) {
                                            var22 = LexoRankOperationOutcome.error(Reason.VALIDATION_FAILED, "gh.api.rank.error.lexorank.fieldlength.exceeded.norebalance");
                                            return var22;
                                        }
                                        //保存rankRow
                                        LockProcessOutcome<ServiceOutcome<LexoRankRow>> saveProcessOutcome = this.dao.save(lock, rankRow);
                                        if (!saveProcessOutcome.isRetry()) {
                                            ServiceOutcome<LexoRankRow> saveOutcome = saveProcessOutcome.get();
                                            if (saveOutcome.isInvalid()) {
                                                LexoRankOperationOutcome var43 = LexoRankOperationOutcome.error(saveOutcome.getErrors());
                                                return var43;
                                            }
                                            LexoRankRow savedRow = saveOutcome.get();
                                            Long savedRankFieldId = savedRow.getFieldId();
                                            Long savedIssueId = savedRow.getIssueId();
                                            LexoRank oldRank = LexoRank.parse(unlockedRankRow.getRank());
                                            LexoRank savedRank = LexoRank.parse(savedRow.getRank());
                                            LexoRankChange lexoRankChange = LexoRankChange.builder().forRankField(savedRankFieldId).rankedIssue(savedIssueId).from(oldRank).to(savedRank).build();
                                            issueIdsToReIndex.add(this.issueToRankIssueId);
                                            LexoRankOperationOutcome var30 = LexoRankOperationOutcome.ok(lexoRankChange, issueIdsToReIndex);
                                            return var30;
                                        }
                                    }
                                }
                            } finally {
                                //释放锁
                                this.dao.releaseLock(lock);
                            }
                        }
                    }
                }
            }
        }

        return LexoRankOperationOutcome.timeout();
    }

    private Optional<LexoRankOperationOutcome<LexoRankChange>> swapRanks(Lock lock, LexoRankRow masterRow, LexoRankRow slaveRow, Collection<Long> issueIdsToReIndex) {
        LexoRank oldSlaveRank = LexoRank.parse(slaveRow.getRank());
        LexoRank oldMasterRank = LexoRank.parse(masterRow.getRank());
        LexoRank between = oldSlaveRank.between(oldMasterRank);
        masterRow.setRank(between.format());
        LockProcessOutcome<ServiceOutcome<LexoRankRow>> betweenSaveResult = this.dao.save(lock, masterRow);
        if (betweenSaveResult.isRetry()) {
            return Optional.empty();
        } else {
            ServiceOutcome<LexoRankRow> betweenOutcome = betweenSaveResult.get();
            if (betweenOutcome.isInvalid()) {
                return Optional.of(LexoRankOperationOutcome.error(betweenOutcome.getErrors()));
            } else {
                slaveRow.setRank(oldMasterRank.format());
                LockProcessOutcome<ServiceOutcome<LexoRankRow>> slaveSaveResults = this.dao.save(lock, slaveRow);
                if (slaveSaveResults.isRetry()) {
                    return Optional.empty();
                } else {
                    ServiceOutcome<LexoRankRow> slaveOutcome = slaveSaveResults.get();
                    if (slaveOutcome.isInvalid()) {
                        return Optional.of(LexoRankOperationOutcome.error(slaveOutcome.getErrors()));
                    } else {
                        masterRow.setRank(oldSlaveRank.format());
                        LockProcessOutcome<ServiceOutcome<LexoRankRow>> masterSaveResult = this.dao.save(lock, masterRow);
                        issueIdsToReIndex.add(masterRow.getIssueId());
                        issueIdsToReIndex.add(slaveRow.getIssueId());
                        ServiceOutcome<LexoRankRow> masterOutcome = masterSaveResult.get();
                        if (masterOutcome.isInvalid()) {
                            return Optional.of(LexoRankOperationOutcome.ok(this.createLexoRankChange(oldMasterRank, (LexoRankRow) betweenOutcome.get()), issueIdsToReIndex));
                        } else {
                            return Optional.of(LexoRankOperationOutcome.ok(this.createLexoRankChange(oldMasterRank, (LexoRankRow) masterOutcome.get()), issueIdsToReIndex));
                        }
                    }
                }
            }
        }
    }

    private LexoRankChange createLexoRankChange(LexoRank oldRank, LexoRankRow savedRow) {
        Long savedRankFieldId = savedRow.getFieldId();
        Long savedIssueId = savedRow.getIssueId();
        LexoRank savedRank = LexoRank.parse(savedRow.getRank());
        return LexoRankChange.builder().forRankField(savedRankFieldId).rankedIssue(savedIssueId).from(oldRank).to(savedRank).build();
    }

    private LexoRank getRankRelativeToOtherIssue(LexoRankRow[] lockedSurroundingRankRows) {
        LexoRank lowerRank = LexoRank.parse(lockedSurroundingRankRows[0].getRank());
        LexoRank higherRank = LexoRank.parse(lockedSurroundingRankRows[1].getRank());
        LexoRank relativeRank;
        if (lowerRank.isMin()) {
            relativeRank = higherRank.genPrev();
        } else if (lowerRank.isMax()) {
            relativeRank = lowerRank.genNext();
        } else {
            relativeRank = lowerRank.between(higherRank, this.remainingRankOperations);
        }

        return relativeRank;
    }

    private LexoRankOperationOutcome<LexoRankChange> rankFirstOrLast() {
        long timeoutTime = System.currentTimeMillis() + (long) LexoRankSettings.RANK_RETRY_TIMEOUT_MS;
        Set<Long> issueIdsToReIndex = Sets.newHashSet();
        BackoffHandler backoffHandler = new BackoffHandler(this.statisticsAgent, timeoutTime);
        while (System.currentTimeMillis() < timeoutTime) {
            backoffHandler.maybeWait();
            Maybe<LexoRankRow> maybeUnlockedLexoRankRow = this.dao.findByFieldAndIssueId(this.rankFieldId.longValue(), this.issueToRankIssueId.longValue());
            if (maybeUnlockedLexoRankRow.isEmpty()) {
                return LexoRankOperationOutcome.reindexRequired();
            }
            LexoRankRow unlockedLexoRankRow = maybeUnlockedLexoRankRow.get();
            LexoRankRow[] unlockedLexoRankRows = this.rankOperationType.equals(LexoRankOperation.RankOperationType.RANK_FIRST) ? this.dao.getMinimumMarkerRowAndNextRow(this.rankFieldId.longValue()) : this.dao.getMaximumMarkerRowAndPreviousRow(this.rankFieldId.longValue());
            LexoRankHealOperation healOperation = LexoRankHealOperation.builder(this.dao, this.statisticsAgent).forRankField(this.rankFieldId).heal(new LexoRankRow[]{unlockedLexoRankRow, unlockedLexoRankRows[0], unlockedLexoRankRows[1]}).build();
            LexoRankOperationOutcome<Boolean> healOperationOutcome = healOperation.execute();
            if (healOperationOutcome.isValid() && healOperationOutcome.getResult()) {
                issueIdsToReIndex.addAll(healOperationOutcome.getIssueIdsToReIndex());
            } else {
                LexoRank rankBetweenFrom = LexoRank.parse(unlockedLexoRankRows[0].getRank());
                LexoRank rankBetweenTo = LexoRank.parse(unlockedLexoRankRows[1].getRank());
                if (!rankBetweenFrom.getBucket().equals(rankBetweenTo.getBucket())) {
                } else {
                    HashSet<LexoRankRow> lexoRankRowsToLock = Sets.newHashSet(unlockedLexoRankRow, unlockedLexoRankRows[0], unlockedLexoRankRows[1]);
                    LockOutcome lockOutcome = this.dao.acquireLock(lexoRankRowsToLock);
                    Lock lock = lockOutcome.get();
                    if (!lockOutcome.isValid()) {
                        if (lock != null) {
                            this.dao.releaseLock(lock);
                        }
                    } else {
                        LexoRankOperationOutcome var22;
                        try {
                            LexoRankRow rankRow = this.dao.getByFieldAndIssueId(this.rankFieldId, this.issueToRankIssueId);
                            if (LexoRankRowUtils.areRowsDifferent(unlockedLexoRankRow, rankRow)) {
                                continue;
                            }
                            LexoRankRow[] lockedRankRows = this.rankOperationType.equals(LexoRankOperation.RankOperationType.RANK_FIRST) ? this.dao.getMinimumMarkerRowAndNextRow(this.rankFieldId.longValue()) : this.dao.getMaximumMarkerRowAndPreviousRow(this.rankFieldId.longValue());
                            if (LexoRankRowUtils.areRowsDifferent(lockedRankRows, unlockedLexoRankRows)) {
                                continue;
                            }
                            LexoRank rankAdjacentToMarkerRow = LexoRank.parse(lockedRankRows[1].getRank());
                            LexoRank newRank = this.rankOperationType.equals(LexoRankOperation.RankOperationType.RANK_FIRST) ? rankAdjacentToMarkerRow.genPrev() : rankAdjacentToMarkerRow.genNext();
                            rankRow.setRank(newRank.format());
                            boolean existsRankForFieldId = this.dao.existsRankForFieldId(this.rankFieldId, newRank.format());
                            LexoRankOperationOutcome var32;
                            if (existsRankForFieldId) {
                                var32 = LexoRankOperationOutcome.duplicateRank();
                                return var32;
                            }
                            if (this.exceedsMaxRankLength(newRank)) {
                                var32 = LexoRankOperationOutcome.error(Reason.VALIDATION_FAILED, "gh.api.rank.error.lexorank.fieldlength.exceeded.norebalance", new Object[0]);
                                return var32;
                            }
                            LockProcessOutcome<ServiceOutcome<LexoRankRow>> saveProcessOutcome = this.dao.save(lock, rankRow);
                            if (saveProcessOutcome.isRetry()) {
                                continue;
                            }
                            ServiceOutcome<LexoRankRow> saveOutcome = saveProcessOutcome.get();
                            if (!saveOutcome.isInvalid()) {
                                LexoRankRow savedRow = saveOutcome.get();
                                Long savedRankFieldId = savedRow.getFieldId();
                                Long savedIssueId = savedRow.getIssueId();
                                LexoRank oldRank = LexoRank.parse(unlockedLexoRankRow.getRank());
                                LexoRank savedRank = LexoRank.parse(savedRow.getRank());
                                LexoRankChange lexoRankChange = LexoRankChange.builder().forRankField(savedRankFieldId).rankedIssue(savedIssueId).from(oldRank).to(savedRank).build();
                                issueIdsToReIndex.add(this.issueToRankIssueId);
                                LexoRankOperationOutcome var28 = LexoRankOperationOutcome.ok(lexoRankChange, issueIdsToReIndex);
                                return var28;
                            }

                            var22 = LexoRankOperationOutcome.error(saveOutcome.getErrors());
                        } finally {
                            this.dao.releaseLock(lock);
                        }

                        return var22;
                    }
                }
            }
        }

        return LexoRankOperationOutcome.timeout();
    }

    private boolean exceedsMaxRankLength(LexoRank rank) {
        return rank.format().length() > LEXORANK_MAX_LENGTH;
    }

    public static LexoRankOperation.IssueToRank builder(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent) {
        return new LexoRankOperation.Builder(lexoRankDao, lexoRankStatisticsAgent);
    }

    static {
        rankOperationToLexoOperationMap.put(LexoRankOperation.RankOperationType.RANK_BEFORE, Operation.RANK_BEFORE);
        rankOperationToLexoOperationMap.put(LexoRankOperation.RankOperationType.RANK_AFTER, Operation.RANK_AFTER);
        rankOperationToLexoOperationMap.put(LexoRankOperation.RankOperationType.RANK_FIRST, Operation.RANK_FIRST);
        rankOperationToLexoOperationMap.put(LexoRankOperation.RankOperationType.RANK_LAST, Operation.RANK_LAST);
        rankOperationToLexoOperationMap.put(LexoRankOperation.RankOperationType.RANK_INITIAL, Operation.RANK_INITIAL);
        rankFieldIdLocks = new ConcurrentHashMap();
    }

    public interface CompleteRankOperation {
        LexoRankOperation build();

        LexoRankOperation.CompleteRankOperation withRemainingRankOperations(int var1);
    }

    public interface ForRankField {
        LexoRankOperation.CompleteRankOperation forRankField(Long var1);
    }

    public interface HowToRankIssue {
        LexoRankOperation.ForRankField beforeIssue(Long var1);

        LexoRankOperation.ForRankField afterIssue(Long var1);

        LexoRankOperation.ForRankField first();

        LexoRankOperation.ForRankField last();

        LexoRankOperation.ForRankField initial();
    }

    public interface IssueToRank {
        LexoRankOperation.HowToRankIssue rankIssue(Long var1);
    }

    public static class Builder implements LexoRankOperation.IssueToRank, LexoRankOperation.HowToRankIssue, LexoRankOperation.ForRankField, LexoRankOperation.CompleteRankOperation {
        private LexoRankDao lexoRankDao;
        private LexoRankStatisticsAgent lexoRankStatisticsAgent;
        private LexoRankOperation.RankOperationType rankOperationType;
        private Long issueToRankIssueId;
        private Long issueToRankAroundIssueId;
        private Long rankFieldId;
        private Integer remainingRankOperations;

        private Builder(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent) {
            this.remainingRankOperations = 0;
            this.lexoRankDao = lexoRankDao;
            this.lexoRankStatisticsAgent = lexoRankStatisticsAgent;
        }

        @Override
        public LexoRankOperation build() {
            return new LexoRankOperation(this.lexoRankDao, this.lexoRankStatisticsAgent, this.rankOperationType, this.issueToRankIssueId, this.issueToRankAroundIssueId, this.rankFieldId, this.remainingRankOperations);
        }

        @Override
        public LexoRankOperation.CompleteRankOperation withRemainingRankOperations(int remainingRankOperations) {
            this.remainingRankOperations = remainingRankOperations;
            return this;
        }

        @Override
        public LexoRankOperation.CompleteRankOperation forRankField(Long rankFieldId) {
            this.rankFieldId = rankFieldId;
            return this;
        }

        @Override
        public LexoRankOperation.HowToRankIssue rankIssue(Long issueId) {
            this.issueToRankIssueId = issueId;
            return this;
        }

        @Override
        public LexoRankOperation.ForRankField beforeIssue(Long issueId) {
            this.rankOperationType = LexoRankOperation.RankOperationType.RANK_BEFORE;
            this.issueToRankAroundIssueId = issueId;
            return this;
        }

        @Override
        public LexoRankOperation.ForRankField afterIssue(Long issueId) {
            this.rankOperationType = LexoRankOperation.RankOperationType.RANK_AFTER;
            this.issueToRankAroundIssueId = issueId;
            return this;
        }

        @Override
        public LexoRankOperation.ForRankField first() {
            this.rankOperationType = LexoRankOperation.RankOperationType.RANK_FIRST;
            return this;
        }

        @Override
        public LexoRankOperation.ForRankField last() {
            this.rankOperationType = LexoRankOperation.RankOperationType.RANK_LAST;
            return this;
        }

        @Override
        public LexoRankOperation.ForRankField initial() {
            this.rankOperationType = LexoRankOperation.RankOperationType.RANK_INITIAL;
            return this;
        }
    }

    public static enum RankOperationType {
        RANK_BEFORE,
        RANK_AFTER,
        RANK_FIRST,
        RANK_LAST,
        RANK_INITIAL;

        private RankOperationType() {
        }
    }
}