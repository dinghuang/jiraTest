package com.example.demo.test;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.greenhopper.global.LoggerWrapper;
import com.atlassian.greenhopper.global.PerformanceLogger;
import com.atlassian.greenhopper.manager.lexorank.LexoRankRow;
import com.atlassian.greenhopper.manager.lexorank.suspend.LexoRankSuspendManager;
import com.atlassian.greenhopper.model.validation.ErrorCollection;
import com.atlassian.greenhopper.service.IssueIndexService;
import com.atlassian.greenhopper.service.ServiceResult;
import com.atlassian.greenhopper.service.ServiceResultImpl;
import com.atlassian.greenhopper.service.lexorank.LexoRankOperationOutcome;
import com.atlassian.greenhopper.service.lexorank.LexoRankStatisticsAgent;
import com.atlassian.jira.bc.ServiceOutcome;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IssueIndexingParams;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
@Service
public class LexoRankBalancer {
    private static final int LAZY_INDEX_QUEUE_LIMIT = 64;
    protected final LoggerWrapper log = LoggerWrapper.with(this.getClass());
    @Autowired
    private LexoRankDao lexoRankDao;
    @Autowired
    private IssueIndexService issueIndexService;
    @Autowired
    private LexoRankStatisticsAgent lexoRankStatisticsAgent;
    @Autowired
    private ClusterLockService lockService;
    @Autowired
    private LexoRankBalancingService lexoRankBalancingService;
    @Autowired
    private LexoRankBalanceRankInfoService lexoRankBalanceRankInfoService;
    @Autowired
    private IssueManager issueManager;
    @Autowired
    private LexoRankBalancerProgressLoggerFactory lexoRankBalancerProgressLoggerFactory;
    @Autowired
    private LexoRankSuspendManager lexoRankSuspendManager;

    public LexoRankBalancer() {
    }

    public ServiceResult fullBalance() {
        this.log.debug("LexoRankBalancer.fullBalance()", new Object[0]);
        Collection<Long> fieldIds = this.lexoRankDao.findFieldIdsInLexoRankTable();
        ServiceResult result = this.balanceFieldIds(fieldIds);
        this.log.debug("LexoRankBalancer.fullBalance() returning " + ToStringBuilder.reflectionToString(result), new Object[0]);
        return result;
    }

    public ServiceResult balanceFieldIds(Collection<Long> fieldIds) {
        Lock lock = this.getLock();
        ErrorCollection errors = new ErrorCollection();
        this.log.debug("Locking lexorank balance with lock=" + lock, new Object[0]);
        if (lock.tryLock()) {
            this.log.info("Balancing %d fields", new Object[]{Integer.valueOf(fieldIds.size())});

            try {
                Iterator var4 = fieldIds.iterator();

                while (var4.hasNext()) {
                    Long fieldId = (Long) var4.next();
                    this.log.debug("Balancing field with id[%d]", new Object[]{fieldId});
                    PerformanceLogger.debug("Balancing field with id %d", new Object[]{fieldId}).measure(() -> {
                        this.lexoRankStatisticsAgent.startOperation(LexoRankStatisticsAgent.Operation.REBALANCE_FIELDID);
                        ServiceResult balanceOutcome = this.balanceFieldId(fieldId.longValue());
                        this.lexoRankStatisticsAgent.endOperation(LexoRankStatisticsAgent.Operation.REBALANCE_FIELDID);
                        if (balanceOutcome.isInvalid()) {
                            errors.addAllErrors(balanceOutcome.getErrors());
                        }

                    });
                }
            } finally {
                lock.unlock();
                this.log.debug("Unlocking lexorank balance lock=" + lock, new Object[0]);
            }

            this.log.info("Balancing of %d fields completed with %d errors", new Object[]{Integer.valueOf(fieldIds.size()), Integer.valueOf(errors.getErrors().size())});
        } else {
            this.log.warn("Failed to aquire cluster lock - balancing already in progress on another node", new Object[0]);
            errors.addError(ErrorCollection.Reason.CONFLICT, "gh.lexorank.balancer.error.in.progress", new Object[0]);
        }

        return ServiceResultImpl.from(errors);
    }

    private boolean isClusterLockAcquired() {
        Lock lock = this.getLock();

        try {
            if (lock.tryLock(0L, TimeUnit.SECONDS)) {
                lock.unlock();
                return false;
            }
        } catch (InterruptedException var3) {
            ;
        }

        return true;
    }

    public LexoRankBalancer.LexoRankBalancerStatus getBalanceStatus(List<CustomField> rankFields) {
        List<LexoRankBalancer.LexoRankBalancerFieldStatus> perFieldStatus = Lists.newArrayList();
        Iterator var3 = rankFields.iterator();

        while (var3.hasNext()) {
            CustomField rankField = (CustomField) var3.next();
            long numRankedIssues = 0L;
            Float percentComplete = null;
            List<Long> distribution = Lists.newArrayList();
            LexoRankBucket[] var9 = LexoRankBucket.values();
            int var10 = var9.length;

            int var11;
            LexoRankBucket bucket;
            long ranksInNextBucket;
            for (var11 = 0; var11 < var10; ++var11) {
                bucket = var9[var11];
                ranksInNextBucket = this.lexoRankDao.getRowCountInBucket(rankField.getIdAsLong(), bucket, LexoRankRow.RankRowType.ISSUE_RANK_ROW);
                distribution.add(Long.valueOf(ranksInNextBucket));
                numRankedIssues += ranksInNextBucket;
            }

            var9 = LexoRankBucket.values();
            var10 = var9.length;

            for (var11 = 0; var11 < var10; ++var11) {
                bucket = var9[var11];
                ranksInNextBucket = ((Long) distribution.get(bucket.next().ordinal())).longValue();
                long ranksInThisBucket = ((Long) distribution.get(bucket.ordinal())).longValue();
                if (ranksInThisBucket > 0L && ranksInNextBucket == 0L) {
                    long sourceBucketCount = ((Long) distribution.get(bucket.prev().ordinal())).longValue();
                    if (sourceBucketCount + ranksInThisBucket > 0L) {
                        percentComplete = Float.valueOf((float) ranksInThisBucket * 100.0F / (float) (sourceBucketCount + ranksInThisBucket));
                        percentComplete = Float.valueOf((float) Math.round(percentComplete.floatValue() * 100.0F) / 100.0F);
                    } else {
                        percentComplete = Float.valueOf(100.0F);
                    }
                }
            }

            perFieldStatus.add(new LexoRankBalancer.LexoRankBalancerFieldStatus(rankField.getFieldName(), rankField.getIdAsLong(), Long.valueOf(numRankedIssues), percentComplete, distribution, this.lexoRankBalanceRankInfoService.getMaxRank(rankField.getIdAsLong())));
        }

        return new LexoRankBalancer.LexoRankBalancerStatus(Boolean.valueOf(this.isClusterLockAcquired()), perFieldStatus);
    }

    private Lock getLock() {
        return this.lockService.getLockForName(LexoRankBalancer.class.getName());
    }

    private ServiceResult balanceFieldId(long rankFieldId) {
        LexoRankRow minimumMarkerRow = this.lexoRankDao.getMinimumMarkerRow(rankFieldId);
        LexoRankRow maximumMarkerRow = this.lexoRankDao.getMaximumMarkerRow(rankFieldId);
        LexoRank minRank = LexoRank.parse(minimumMarkerRow.getRank());
        LexoRank maxRank = LexoRank.parse(maximumMarkerRow.getRank());
        LexoRank baseMarkerRank = null;
        LexoRankBucket nextBucket = null;
        long numRowsForField = (long) this.lexoRankDao.getRowCountForFieldId(Long.valueOf(rankFieldId)).intValue();
        LexoRankBalancerProgressLogger lexoRankBalancerProgressLogger;
        LexoRankBucket minimumMarkerBucket;
        if (minRank.getBucket().equals(maxRank.getBucket())) {
            lexoRankBalancerProgressLogger = this.lexoRankBalancerProgressLoggerFactory.create(rankFieldId, numRowsForField, 0L);
            this.log.debug("Moving marker row to start of rebalancing", new Object[0]);
            minimumMarkerBucket = minRank.getBucket();
            LexoRankBalanceOperation balanceOperation;
            LexoRankOperationOutcome balanceOperationOutcome;
            LexoRankBalanceChange balanceChange;
            switch (minimumMarkerBucket.ordinal()) {
                case 1:
                case 2:
                    this.log.debug("Moving maximum marker row to next bucket", new Object[0]);
                    balanceOperation = LexoRankBalanceOperation.builder(this.lexoRankDao, this.lexoRankStatisticsAgent).balanceField(Long.valueOf(rankFieldId)).moveMaximumMarkerRow().moveToBucket(maxRank.getBucket().next()).build();
                    balanceOperationOutcome = balanceOperation.execute();
                    if (!balanceOperationOutcome.isValid()) {
                        return ServiceResultImpl.from(balanceOperationOutcome.getErrors());
                    }

                    lexoRankBalancerProgressLogger.makeProgress();
                    balanceChange = (LexoRankBalanceChange) balanceOperationOutcome.getResult();
                    maxRank = balanceChange.getNewRank();
                    nextBucket = balanceChange.getNewBucket();
                    baseMarkerRank = maxRank;
                    break;
                case 3:
                    this.log.debug("Moving minimum marker row from bucket 2 to bucket 0", new Object[0]);
                    balanceOperation = LexoRankBalanceOperation.builder(this.lexoRankDao, this.lexoRankStatisticsAgent).balanceField(Long.valueOf(rankFieldId)).moveMinimumMarkerRow().moveToBucket(maxRank.getBucket().next()).build();
                    balanceOperationOutcome = balanceOperation.execute();
                    if (!balanceOperationOutcome.isValid()) {
                        return ServiceResultImpl.from(balanceOperationOutcome.getErrors());
                    }

                    lexoRankBalancerProgressLogger.makeProgress();
                    balanceChange = (LexoRankBalanceChange) balanceOperationOutcome.getResult();
                    minRank = balanceChange.getNewRank();
                    nextBucket = balanceChange.getNewBucket();
                    baseMarkerRank = minRank;
                    break;
                default:
                    return ServiceResultImpl.error(ErrorCollection.Reason.SERVER_ERROR, "gh.lexorank.balancer.error.nobucket", new Object[]{minimumMarkerBucket});
            }
        } else {
            minimumMarkerBucket = minRank.getBucket();
            LexoRankBucket maximumMarkerBucket = maxRank.getBucket();
            if (minimumMarkerBucket.equals(LexoRankBucket.BUCKET_0) && maximumMarkerBucket.equals(LexoRankBucket.BUCKET_2)) {
                nextBucket = LexoRankBucket.BUCKET_0;
                baseMarkerRank = minRank;
            } else if (minimumMarkerBucket.equals(LexoRankBucket.BUCKET_0) && maximumMarkerBucket.equals(LexoRankBucket.BUCKET_1)) {
                nextBucket = LexoRankBucket.BUCKET_1;
                baseMarkerRank = maxRank;
            } else if (minimumMarkerBucket.equals(LexoRankBucket.BUCKET_1) && maximumMarkerBucket.equals(LexoRankBucket.BUCKET_2)) {
                nextBucket = LexoRankBucket.BUCKET_2;
                baseMarkerRank = maxRank;
            }

            long numRowsBalanced = this.lexoRankDao.getRowCountInBucket(Long.valueOf(rankFieldId), nextBucket);
            lexoRankBalancerProgressLogger = this.lexoRankBalancerProgressLoggerFactory.create(rankFieldId, numRowsForField, numRowsBalanced);
        }

        if (!this.verifyMarkerRanksInBalanceableState(minRank, maxRank)) {
            this.log.debug("The rank rows for rank field [id=%s] are not in a balanceable state", new Object[]{Long.valueOf(rankFieldId)});
            return ServiceResultImpl.error(ErrorCollection.Reason.SERVER_ERROR, "gh.lexorank.balancer.error.nocombination", new Object[]{minRank.getBucket(), maxRank.getBucket()});
        } else {
            BlockingQueue<Issue> indexingQueue = new LinkedBlockingQueue(64);
            ExecutorService indexer = Executors.newSingleThreadExecutor(ThreadFactories.namedThreadFactory("LexoRankReindexer"));

            try {
                indexer.submit(new LexoRankBalancer.IndexingRunnable(indexingQueue, indexer));

                while (true) {
                    ServiceResult balancingStaus;
                    if ((balancingStaus = this.getBalancingStatus(indexer, rankFieldId)).isValid()) {
                        this.log.debug("Balancing next rank row for rank field [id=%s]", new Object[]{Long.valueOf(rankFieldId)});
                        LexoRankBalanceOperation balanceOperation = LexoRankBalanceOperation.builder(this.lexoRankDao, this.lexoRankStatisticsAgent).balanceField(Long.valueOf(rankFieldId)).moveNextRankRow().moveToBucket(nextBucket).build();
                        LexoRankOperationOutcome<LexoRankBalanceChange> balanceOperationOutcome = balanceOperation.execute();
                        if (!balanceOperationOutcome.isValid()) {
                            ServiceResult var33 = ServiceResultImpl.from(balanceOperationOutcome.getErrors());
                            return var33;
                        }

                        lexoRankBalancerProgressLogger.makeProgress();
                        LexoRankBalanceChange balanceChange = (LexoRankBalanceChange) balanceOperationOutcome.getResult();
                        Long issueId = balanceChange.getIssueId();
                        if (issueId != null && !balanceChange.isVirtualIssue()) {
                            Issue issue = this.issueManager.getIssueObject(issueId);
                            if (issue == null) {
                                this.log.debug("Detected a rank for an issue that doesn't exist. Deleting all ranks for issue[id=%s]", new Object[]{issueId});
                                this.deleteRanksForIssue(issueId);
                            } else {
                                this.log.debug("ReIndexing issue[id=%s]", new Object[]{issueId});

                                try {
                                    indexingQueue.put(issue);
                                } catch (InterruptedException var24) {
                                    this.log.error("Unable to reindex rank for issue - " + issue.getKey(), new Object[0]);
                                    throw new RuntimeException(var24);
                                }
                            }
                        }

                        if (!this.checkIfComplete(balanceChange.getNewRank(), baseMarkerRank)) {
                            continue;
                        }

                        this.log.debug("Balancing of rank rows for rank field [id=%s] is complete", new Object[]{Long.valueOf(rankFieldId)});
                        balancingStaus = ServiceResultImpl.ok();
                    }

                    this.shutDownIndexer(indexer);
                    ServiceResult var32 = balancingStaus;
                    return var32;
                }
            } finally {
                indexer.shutdownNow();
            }
        }
    }

    private ServiceResult getBalancingStatus(ExecutorService indexer, long rankFieldId) {
        if (!this.lexoRankBalancingService.isBalancingDisabled() && !indexer.isShutdown()) {
            if (this.lexoRankBalancingService.shouldBalancingBackOff()) {
                this.log.debug("Balancing for rank field [id=%s] is terminating - backoff", new Object[]{Long.valueOf(rankFieldId)});
                return ServiceResultImpl.error(ErrorCollection.Reason.SERVER_ERROR, "gh.lexorank.service.error.balancing.backoff", new Object[0]);
            } else if (this.lexoRankSuspendManager.isSuspended()) {
                this.log.debug("Balancing for rank field [id=%s] is terminating - suspended by user", new Object[]{Long.valueOf(rankFieldId)});
                return ServiceResultImpl.error(ErrorCollection.Reason.CONFLICT, "gh.lexorank.balancer.error.suspended", new Object[0]);
            } else {
                return ServiceResultImpl.ok();
            }
        } else {
            this.log.debug("Balancing for rank field [id=%s] is terminating", new Object[]{Long.valueOf(rankFieldId)});
            return ServiceResultImpl.error(ErrorCollection.Reason.SERVER_ERROR, "gh.lexorank.balancer.error.plugin.terminating", new Object[0]);
        }
    }

    private void shutDownIndexer(ExecutorService indexer) {
        indexer.shutdown();

        try {
            if (!indexer.awaitTermination(320L, TimeUnit.SECONDS)) {
                indexer.shutdownNow();
                if (!indexer.awaitTermination(60L, TimeUnit.SECONDS)) {
                    this.log.error("Indexing executor for LexoRank balance did not terminate.", new Object[0]);
                }
            }
        } catch (InterruptedException var3) {
            this.log.error("LexoRank indexing termination was interrupted.", new Object[0]);
        }

    }

    private void deleteRanksForIssue(Long issueId) {
        LexoRankDeleteOperation deleteOperation = LexoRankDeleteOperation.builder(this.lexoRankDao, this.lexoRankStatisticsAgent).forIssue(issueId).build();
        deleteOperation.execute();
    }

    private boolean verifyMarkerRanksInBalanceableState(LexoRank minRank, LexoRank maxRank) {
        LexoRankBucket minRankBucket = minRank.getBucket();
        LexoRankBucket maxRankBucket = maxRank.getBucket();
        return minRankBucket.equals(LexoRankBucket.BUCKET_0) && maxRankBucket.equals(LexoRankBucket.BUCKET_1) ? true : (minRankBucket.equals(LexoRankBucket.BUCKET_1) && maxRankBucket.equals(LexoRankBucket.BUCKET_2) ? true : minRankBucket.equals(LexoRankBucket.BUCKET_0) && maxRankBucket.equals(LexoRankBucket.BUCKET_2));
    }

    private boolean checkIfComplete(LexoRank rank, LexoRank baseRank) {
        return baseRank.isMin() && rank.isMax() || baseRank.isMax() && rank.isMin();
    }

    private class IndexingRunnable implements Runnable {
        private final BlockingQueue<Issue> indexingQueue;
        private final ExecutorService indexer;

        public IndexingRunnable(BlockingQueue<Issue> var1, ExecutorService indexingQueue) {
            this.indexingQueue = var1;
            this.indexer = indexingQueue;
        }

        @Override
        public void run() {
            while (!this.indexer.isShutdown() || !this.indexingQueue.isEmpty()) {
                if (LexoRankBalancer.this.lexoRankBalancingService.isBalancingDisabled()) {
                    LexoRankBalancer.this.log.warn("Balancing disabled while balance indexing was in progress. All issues may not be properly indexed.", new Object[0]);
                    break;
                } else {
                    List<Issue> issuesToIndex = new ArrayList();
                    Issue issue = null;

                    try {
                        issue = (Issue) this.indexingQueue.poll(1L, TimeUnit.SECONDS);
                    } catch (InterruptedException var5) {
                        LexoRankBalancer.this.log.error("Indexing for LexoRank re-balancing was interrupted.", new Object[0]);
                    }

                    if (issue != null) {
                        issuesToIndex.add(issue);
                        this.indexingQueue.drainTo(issuesToIndex);
                        IssueIndexingParams params = IssueIndexingParams.builder().setComments(false).setChangeHistory(false).build();
                        ServiceOutcome<Void> outcome = LexoRankBalancer.this.issueIndexService.reindexIssueAndSubtasks(issuesToIndex, params);
                        if (!outcome.isValid()) {
                            LexoRankBalancer.this.log.error("Indexing failed, will abort indexing any more issues in the balancer", new Object[0]);
                            LexoRankBalancer.this.log.error(ErrorCollection.fromJiraErrorCollection(outcome.getErrorCollection()));
                            break;
                        }
                    }
                }
            }

            this.indexer.shutdown();
            this.indexingQueue.clear();
        }
    }

    public static class LexoRankBalancerFieldStatus {
        public final String fieldName;
        public final Long fieldId;
        public final Long numRankedIssues;
        public final Float percentComplete;
        public final List<Long> distribution;
        public final LexoRankBalanceRankInfoService.LexoRankMaxRank maxRank;

        LexoRankBalancerFieldStatus(String fieldName, Long fieldId, Long numRankedIssues, Float percentComplete, List<Long> distribution, LexoRankBalanceRankInfoService.LexoRankMaxRank maxRank) {
            this.fieldName = fieldName;
            this.fieldId = fieldId;
            this.numRankedIssues = numRankedIssues;
            this.percentComplete = percentComplete;
            this.distribution = distribution;
            this.maxRank = maxRank;
        }
    }

    public static class LexoRankBalancerStatus {
        public final Boolean balancerLocked;
        public final List<LexoRankBalancer.LexoRankBalancerFieldStatus> perFieldStatus;

        public LexoRankBalancerStatus(Boolean balancerLocked, List<LexoRankBalancer.LexoRankBalancerFieldStatus> perFieldStatus) {
            this.balancerLocked = balancerLocked;
            this.perFieldStatus = perFieldStatus;
        }
    }
}