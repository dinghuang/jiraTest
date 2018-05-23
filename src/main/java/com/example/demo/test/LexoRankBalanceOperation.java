package com.example.demo.test;

import com.atlassian.greenhopper.global.LoggerWrapper;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRow;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRowUtils;
import com.atlassian.greenhopper.manager.lexorank.lock.Lock;
import com.atlassian.greenhopper.manager.lexorank.lock.LockOutcome;
import com.atlassian.greenhopper.manager.lexorank.lock.LockProcessOutcome;
import com.atlassian.greenhopper.model.validation.ErrorCollection;
import com.atlassian.greenhopper.service.ServiceOutcome;
import com.atlassian.greenhopper.service.lexorank.BackoffHandler;
import com.atlassian.greenhopper.service.lexorank.LexoRankOperationOutcome;
import com.atlassian.greenhopper.service.lexorank.LexoRankSettings;
import com.atlassian.greenhopper.service.lexorank.LexoRankStatisticsAgent;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoRankBalanceOperation {
    private static final LoggerWrapper LOG = LoggerWrapper.with(LexoRankBalanceOperation.class);
    private static final Map<LexoRankBalanceOperation.BalanceOperationType, LexoRankStatisticsAgent.Operation> balanceOperationToLexoOperationMap = Maps.newHashMap();
    private final LexoRankDao dao;
    private final LexoRankStatisticsAgent statisticsAgent;
    private final Long rankFieldId;
    private final LexoRankBalanceOperation.BalanceOperationType balanceOperationType;
    private final LexoRankBucket newBucket;

    private LexoRankBalanceOperation(LexoRankDao dao, LexoRankStatisticsAgent statisticsAgent, Long rankFieldId, LexoRankBalanceOperation.BalanceOperationType balanceOperationType, LexoRankBucket bucket) {
        this.dao = dao;
        this.statisticsAgent = statisticsAgent;
        this.rankFieldId = rankFieldId;
        this.balanceOperationType = balanceOperationType;
        this.newBucket = bucket;
    }

    public static LexoRankBalanceOperation.FieldToBalance builder(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent) {
        return new LexoRankBalanceOperation.Builder(lexoRankDao, lexoRankStatisticsAgent);
    }

    public LexoRankOperationOutcome<LexoRankBalanceChange> execute() {
        this.logStartOfOperation();
        LexoRankOperationOutcome<LexoRankBalanceChange> operationOutcome = null;
        switch (this.balanceOperationType.ordinal()) {
            case 1:
            case 2:
                operationOutcome = this.moveMarkerRowToNextBucket();
                break;
            case 3:
                operationOutcome = this.moveNextRowToNextBucket();
        }

        this.logEndOfOperation();
        return operationOutcome;
    }

    private void logStartOfOperation() {
        this.statisticsAgent.startOperation((LexoRankStatisticsAgent.Operation) balanceOperationToLexoOperationMap.get(this.balanceOperationType));
    }

    private void logEndOfOperation() {
        this.statisticsAgent.endOperation((LexoRankStatisticsAgent.Operation) balanceOperationToLexoOperationMap.get(this.balanceOperationType));
    }

    private LexoRankOperationOutcome<LexoRankBalanceChange> moveNextRowToNextBucket() {
        long timeoutTime = System.currentTimeMillis() + (long) LexoRankSettings.BALANCE_RETRY_TIMEOUT_MS;
        BackoffHandler backoffHandler = new BackoffHandler(this.statisticsAgent, timeoutTime);
        LexoRankBucket oldBucket = this.newBucket.prev();
        LOG.debug("Balancing next rank row to migrate from bucket[%s] to bucket[%s] for rank field[id=%s]", new Object[]{oldBucket.format(), this.newBucket.format(), this.rankFieldId});

        while (System.currentTimeMillis() < timeoutTime) {
            backoffHandler.maybeWait();
            LexoRankRow[] rowsAtBalanceBoundary = this.dao.getRowsAtBalanceBoundaryForFieldId(this.rankFieldId, oldBucket, this.newBucket);
            LOG.debug("Fetched rows at balance boundary", new Object[0]);
            if (rowsAtBalanceBoundary.length != 2) {
                LOG.debug("Couldn't find the balance boundary rows. Got %s rows", new Object[]{Integer.valueOf(rowsAtBalanceBoundary.length)});
                return LexoRankOperationOutcome.error(ErrorCollection.Reason.SERVER_ERROR, "Couldn't find rows on balance boundary", new Object[0]);
            }

            LOG.debug("Fetched rows at balance boundary", new Object[0]);
            LOG.debug("\trowToMigrate : %s", new Object[]{rowsAtBalanceBoundary[0]});
            LOG.debug("\trowLastMigrated : %s", new Object[]{rowsAtBalanceBoundary[1]});
            LexoRankRow rowToBeMigrated = rowsAtBalanceBoundary[0];
            LexoRankRow rowLastMigrated = rowsAtBalanceBoundary[1];
            LexoRank rankToBeMigrated = LexoRank.parse(rowToBeMigrated.getRank());
            LexoRank rankLastMigrated = LexoRank.parse(rowLastMigrated.getRank());
            if (!rankToBeMigrated.getBucket().equals(oldBucket)) {
                LOG.error("Expected the row to migrate to be in the old bucket [oldBucket=%s, rowBucket=%s]", new Object[]{oldBucket.format(), rankToBeMigrated.getBucket().format()});
                return LexoRankOperationOutcome.error(ErrorCollection.Reason.SERVER_ERROR, "Expected the row to migrate to be in the old bucket", new Object[0]);
            }

            if (!rankLastMigrated.getBucket().equals(this.newBucket)) {
                LOG.error("Expected the row last migrated to be in the new bucket [newBucket=%s, rowBucket=%s]", new Object[]{this.newBucket.format(), rankLastMigrated.getBucket().format()});
                return LexoRankOperationOutcome.error(ErrorCollection.Reason.SERVER_ERROR, "Expected the row last migrated to be in the new bucket", new Object[0]);
            }

            LockOutcome lockOutcome = this.dao.acquireLock(rowsAtBalanceBoundary);
            if (lockOutcome.isInvalid()) {
                if (!lockOutcome.isFailRetry()) {
                    LOG.debug("Failed to acquire lock on rows [reason=%s], not recoverable", new Object[]{lockOutcome.getFailDetails()});
                    return LexoRankOperationOutcome.error(ErrorCollection.Reason.SERVER_ERROR, lockOutcome.getFailDetails(), new Object[0]);
                }

                LOG.debug("Failed to acquire lock on rows [reason=%s], trying to again", new Object[]{lockOutcome.getFailDetails()});
            } else {
                Lock lock = lockOutcome.get();

                try {
                    LOG.debug("Acquired lock on rows", new Object[0]);
                    LexoRankRow[] lockedRowsAtBalanceBoundary = this.dao.getRowsAtBalanceBoundaryForFieldId(this.rankFieldId, oldBucket, this.newBucket);
                    if (lockedRowsAtBalanceBoundary.length != 2) {
                        LOG.debug("Couldn't find the balance boundary rows. Got %s rows", new Object[]{Integer.valueOf(rowsAtBalanceBoundary.length)});
                        LexoRankOperationOutcome var24 = LexoRankOperationOutcome.error(ErrorCollection.Reason.SERVER_ERROR, "Couldn't find rows on balance boundary", new Object[0]);
                        return var24;
                    }

                    if (LexoRankRowUtils.areRowsDifferent(rowsAtBalanceBoundary, lockedRowsAtBalanceBoundary)) {
                        LOG.debug("Rows at the balance boundary have changed since we acquired the lock, retry", new Object[0]);
                    } else {
                        LexoRank newRank = null;
                        LexoRank oldRank;
                        switch (rowToBeMigrated.getType().ordinal()) {
                            case 1:
                            case 2:
                                oldRank = LexoRank.parse(rowToBeMigrated.getRank());
                                newRank = oldRank.inNextBucket();
                                break;
                            case 3:
                                LexoRank rankOfLastRowMigrated = LexoRank.parse(rowLastMigrated.getRank());
                                newRank = this.getNewRankForRowToBeMigrated(oldBucket, rankOfLastRowMigrated);
                                break;
                            default:
                                throw new IllegalStateException("Unknown rank row type");
                        }

                        oldRank = LexoRank.parse(rowToBeMigrated.getRank());
                        LOG.debug("Balancing rank row [type=%s, oldRank=%s, newRank=%s]", new Object[]{rowToBeMigrated.getType().name(), oldRank.format(), newRank.format()});
                        rowToBeMigrated.setRank(newRank.format());
                        boolean existsRankForFieldId = this.dao.existsRankForFieldId(this.rankFieldId, rowToBeMigrated.getRank());
                        if (existsRankForFieldId) {
                            LOG.debug("New rank[%s] for issue[id=%s] for rank field[id=%s] already exists, retrying balance oepration", new Object[]{newRank.format(), rowToBeMigrated.getIssueId(), this.rankFieldId});
                        } else {
                            LockProcessOutcome<ServiceOutcome<LexoRankRow>> lockedSaveOutcome = this.dao.save(lock, rowToBeMigrated);
                            if (lockedSaveOutcome.isRetry()) {
                                LOG.debug("Failed to save rank row, retry", new Object[0]);
                            } else {
                                ServiceOutcome<LexoRankRow> moveOutcome = (ServiceOutcome) lockedSaveOutcome.get();
                                if (moveOutcome.isInvalid()) {
                                    LOG.debug("Failed to save rank row, aborting", new Object[0]);
                                    LexoRankOperationOutcome var26 = LexoRankOperationOutcome.error(moveOutcome.getErrors());
                                    return var26;
                                }

                                LOG.debug("Successfully saved rank row", new Object[0]);
                                LexoRankBalanceChange balanceChange;
                                LexoRankOperationOutcome var20;
                                switch (rowToBeMigrated.getType().ordinal()) {
                                    case 1:
                                        balanceChange = LexoRankBalanceChange.builder().forRankField(this.rankFieldId).movedMinimumMarkerRow().movedFromBucket(oldBucket).movedToBucket(this.newBucket).changedRankFrom(oldRank).changedRankTo(newRank).build();
                                        var20 = LexoRankOperationOutcome.ok(balanceChange);
                                        return var20;
                                    case 2:
                                        balanceChange = LexoRankBalanceChange.builder().forRankField(this.rankFieldId).movedMinimumMarkerRow().movedFromBucket(oldBucket).movedToBucket(this.newBucket).changedRankFrom(oldRank).changedRankTo(newRank).build();
                                        var20 = LexoRankOperationOutcome.ok(balanceChange);
                                        return var20;
                                    case 3:
                                        Long issueId = rowToBeMigrated.getIssueId();
                                        balanceChange = LexoRankBalanceChange.builder().forRankField(this.rankFieldId).movedIssueRow().forIssue(issueId).movedFromBucket(oldBucket).movedToBucket(this.newBucket).changedRankFrom(oldRank).changedRankTo(newRank).build();
                                        var20 = LexoRankOperationOutcome.ok(balanceChange, new Long[]{issueId});
                                        return var20;
                                }
                            }
                        }
                    }
                } finally {
                    LOG.debug("Releasing lock", new Object[0]);
                    this.dao.releaseLock(lock);
                }
            }
        }

        LOG.debug("balance operation timed out", new Object[0]);
        return LexoRankOperationOutcome.timeout();
    }

    private LexoRank getNewRankForRowToBeMigrated(LexoRankBucket oldBucket, LexoRank rankOfLastRowMigrated) {
        LexoRank newRank;
        if (!rankOfLastRowMigrated.isMin() && !rankOfLastRowMigrated.isMax()) {
            boolean migrationOrderIsSmallToLarge = oldBucket.compareTo(this.newBucket) > 0;
            newRank = migrationOrderIsSmallToLarge ? rankOfLastRowMigrated.genNext() : rankOfLastRowMigrated.genPrev();
        } else {
            newRank = LexoRank.from(this.newBucket, LexoRank.MID_DECIMAL);
        }

        return newRank;
    }

    private LexoRankOperationOutcome<LexoRankBalanceChange> moveMarkerRowToNextBucket() {
        long timeoutTime = System.currentTimeMillis() + (long) LexoRankSettings.BALANCE_RETRY_TIMEOUT_MS;
        BackoffHandler backoffHandler = new BackoffHandler(this.statisticsAgent, timeoutTime);

        while (true) {
            while (System.currentTimeMillis() < timeoutTime) {
                backoffHandler.maybeWait();
                LexoRankRow[] unlockedRankRows = this.balanceOperationType.equals(LexoRankBalanceOperation.BalanceOperationType.MOVE_MAX) ? this.dao.getMaximumMarkerRowAndPreviousRow(this.rankFieldId.longValue()) : this.dao.getMinimumMarkerRowAndNextRow(this.rankFieldId.longValue());
                Set<LexoRankRow> rowsToLock = Sets.newHashSet(new LexoRankRow[]{unlockedRankRows[0], unlockedRankRows[1]});
                LockOutcome lockOutcome = this.dao.acquireLock(rowsToLock);
                Lock lock = lockOutcome.get();
                if (lockOutcome.isValid()) {
                    try {
                        LexoRankRow[] lockedRankRows = this.balanceOperationType.equals(LexoRankBalanceOperation.BalanceOperationType.MOVE_MAX) ? this.dao.getMaximumMarkerRowAndPreviousRow(this.rankFieldId.longValue()) : this.dao.getMinimumMarkerRowAndNextRow(this.rankFieldId.longValue());
                        if (!LexoRankRowUtils.areRowsDifferent(lockedRankRows, unlockedRankRows)) {
                            LexoRankRow markerRowToMigrate = lockedRankRows[0];
                            LexoRank oldRank = LexoRank.parse(markerRowToMigrate.getRank());
                            LexoRank newRank = oldRank.inNextBucket();
                            if (!newRank.getBucket().equals(this.newBucket)) {
                                throw new IllegalStateException("The new rank's bucket is not the same as the expected next bucket");
                            }

                            markerRowToMigrate.setRank(newRank.format());
                            boolean existsRankForFieldId = this.dao.existsRankForFieldId(this.rankFieldId, markerRowToMigrate.getRank());
                            if (existsRankForFieldId) {
                                LOG.debug("New rank[%s] for marker row for rank field[id=%s] already exists, retrying balance oepration", new Object[]{newRank.format(), this.rankFieldId});
                            } else {
                                LockProcessOutcome<ServiceOutcome<LexoRankRow>> saveResult = this.dao.save(lock, markerRowToMigrate);
                                if (!saveResult.isRetry()) {
                                    LexoRankBalanceChange balanceChange = LexoRankBalanceChange.builder().forRankField(this.rankFieldId).moveMarkerRow(this.balanceOperationType).movedFromBucket(oldRank.getBucket()).movedToBucket(this.newBucket).changedRankFrom(oldRank).changedRankTo(newRank).build();
                                    LexoRankOperationOutcome var15 = LexoRankOperationOutcome.ok(balanceChange);
                                    return var15;
                                }
                            }
                        }
                    } finally {
                        this.dao.releaseLock(lock);
                    }
                } else if (lock != null) {
                    this.dao.releaseLock(lock);
                }
            }

            return LexoRankOperationOutcome.timeout();
        }
    }

    static {
        balanceOperationToLexoOperationMap.put(LexoRankBalanceOperation.BalanceOperationType.MOVE_MAX, LexoRankStatisticsAgent.Operation.MAX_TO_NEXT_BUCKET);
        balanceOperationToLexoOperationMap.put(LexoRankBalanceOperation.BalanceOperationType.MOVE_MIN, LexoRankStatisticsAgent.Operation.MIN_TO_NEXT_BUCKET);
    }

    public interface CompleteBalanceOperation {
        LexoRankBalanceOperation build();
    }

    public interface BucketToMigrateTo {
        LexoRankBalanceOperation.CompleteBalanceOperation moveToBucket(LexoRankBucket var1);
    }

    public interface TypeOfBalanceOperation {
        LexoRankBalanceOperation.BucketToMigrateTo moveMaximumMarkerRow();

        LexoRankBalanceOperation.BucketToMigrateTo moveMinimumMarkerRow();

        LexoRankBalanceOperation.BucketToMigrateTo moveNextRankRow();
    }

    public interface FieldToBalance {
        LexoRankBalanceOperation.TypeOfBalanceOperation balanceField(Long var1);
    }

    private static class Builder implements LexoRankBalanceOperation.FieldToBalance, LexoRankBalanceOperation.TypeOfBalanceOperation, LexoRankBalanceOperation.BucketToMigrateTo, LexoRankBalanceOperation.CompleteBalanceOperation {
        private LexoRankDao lexoRankDao;
        private LexoRankStatisticsAgent lexoRankStatisticsAgent;
        private LexoRankBalanceOperation.BalanceOperationType balanceOperationType;
        private Long rankFieldId;
        private LexoRankBucket newBucket;

        private Builder(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent) {
            this.lexoRankDao = lexoRankDao;
            this.lexoRankStatisticsAgent = lexoRankStatisticsAgent;
        }

        @Override
        public LexoRankBalanceOperation build() {
            return new LexoRankBalanceOperation(this.lexoRankDao, this.lexoRankStatisticsAgent, this.rankFieldId, this.balanceOperationType, this.newBucket);
        }

        @Override
        public LexoRankBalanceOperation.TypeOfBalanceOperation balanceField(Long rankFieldId) {
            this.rankFieldId = rankFieldId;
            return this;
        }

        @Override
        public LexoRankBalanceOperation.BucketToMigrateTo moveMaximumMarkerRow() {
            this.balanceOperationType = LexoRankBalanceOperation.BalanceOperationType.MOVE_MAX;
            return this;
        }

        @Override
        public LexoRankBalanceOperation.BucketToMigrateTo moveMinimumMarkerRow() {
            this.balanceOperationType = LexoRankBalanceOperation.BalanceOperationType.MOVE_MIN;
            return this;
        }

        @Override
        public LexoRankBalanceOperation.CompleteBalanceOperation moveToBucket(LexoRankBucket bucket) {
            this.newBucket = bucket;
            return this;
        }

        @Override
        public LexoRankBalanceOperation.BucketToMigrateTo moveNextRankRow() {
            this.balanceOperationType = LexoRankBalanceOperation.BalanceOperationType.MOVE_NEXT;
            return this;
        }
    }

    public enum BalanceOperationType {
        MOVE_MAX,
        MOVE_MIN,
        MOVE_NEXT;

        BalanceOperationType() {
        }
    }
}
