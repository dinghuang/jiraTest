package com.example.demo.test;

import com.atlassian.greenhopper.manager.lexorank.*;
import com.atlassian.greenhopper.manager.lexorank.lock.Lock;
import com.atlassian.greenhopper.manager.lexorank.lock.LockOutcome;
import com.atlassian.greenhopper.manager.lexorank.lock.LockProcessOutcome;
import com.atlassian.greenhopper.service.ServiceOutcome;
import com.atlassian.greenhopper.service.lexorank.BackoffHandler;
import com.atlassian.greenhopper.service.lexorank.LexoRankOperationOutcome;
import com.atlassian.greenhopper.service.lexorank.LexoRankSettings;
import com.atlassian.greenhopper.service.lexorank.LexoRankStatisticsAgent;
import com.google.common.collect.Lists;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoRankDeleteOperation {
    private LexoRankDao lexoRankDao;
    private LexoRankStatisticsAgent lexoRankStatisticsAgent;
    private LexoRankDeleteOperation.DeleteOperationType deleteOperationType;
    private Long objectTypeId;

    private LexoRankDeleteOperation(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent, LexoRankDeleteOperation.DeleteOperationType deleteOperationType, Long objectTypeId) {
        this.lexoRankDao = lexoRankDao;
        this.lexoRankStatisticsAgent = lexoRankStatisticsAgent;
        this.deleteOperationType = deleteOperationType;
        this.objectTypeId = objectTypeId;
    }

    public static LexoRankDeleteOperation.ForIssueOrRankField builder(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent) {
        return new LexoRankDeleteOperation.Builder(lexoRankDao, lexoRankStatisticsAgent);
    }

    public LexoRankOperationOutcome<Void> execute() {
        this.lexoRankStatisticsAgent.startOperation(LexoRankStatisticsAgent.Operation.DELETE_RANK_FOR_ISSUE);
        LexoRankOperationOutcome<Void> deleteOperationOutcome = null;
        switch (this.deleteOperationType.ordinal()) {
            case 1:
                deleteOperationOutcome = this.deleteRanksForIssue(this.objectTypeId.longValue());
                this.lexoRankStatisticsAgent.endOperation(LexoRankStatisticsAgent.Operation.DELETE_RANK_FOR_ISSUE);
                return deleteOperationOutcome;
            case 2:
            default:
                throw new IllegalArgumentException("Unsupported delete operation type");
        }
    }

    private LexoRankOperationOutcome<Void> deleteRanksForIssue(long issueId) {
        long timeoutTime = System.currentTimeMillis() + (long) LexoRankSettings.RANK_RETRY_TIMEOUT_MS;
        BackoffHandler backoffHandler = new BackoffHandler(this.lexoRankStatisticsAgent, timeoutTime);

        while (true) {
            while (System.currentTimeMillis() < timeoutTime) {
                backoffHandler.maybeWait();
                LexoRankRow[] unlockedRows = this.lexoRankDao.findByIssueId(issueId);
                LockOutcome lockOutcome = this.lexoRankDao.acquireLock(unlockedRows);
                Lock lock = lockOutcome.get();
                if (lockOutcome.isValid()) {
                    LexoRankOperationOutcome var12;
                    try {
                        LexoRankRow[] lockedRows = this.lexoRankDao.findByIssueId(issueId);
                        if (LexoRankRowUtils.areRowsDifferent(lockedRows, unlockedRows)) {
                            continue;
                        }

                        LockProcessOutcome<ServiceOutcome<Void>> deleteProcessOutcome = this.lexoRankDao.deleteByIssueId(lock, Long.valueOf(issueId));
                        if (deleteProcessOutcome.isRetry()) {
                            continue;
                        }

                        ServiceOutcome<Void> deleteOutcome = (ServiceOutcome) deleteProcessOutcome.get();
                        if (!deleteOutcome.isInvalid()) {
                            var12 = LexoRankOperationOutcome.ok((Object) null, Lists.newArrayList(new Long[]{Long.valueOf(issueId)}));
                            return var12;
                        }

                        var12 = LexoRankOperationOutcome.error(deleteOutcome.getErrors());
                    } finally {
                        this.lexoRankDao.releaseLock(lock);
                    }

                    return var12;
                } else if (lock != null) {
                    this.lexoRankDao.releaseLock(lock);
                }
            }

            return LexoRankOperationOutcome.timeout();
        }
    }

    public interface CompleteDeleteOperation {
        LexoRankDeleteOperation build();
    }

    public interface ForIssueOrRankField {
        LexoRankDeleteOperation.CompleteDeleteOperation forIssue(Long var1);

        LexoRankDeleteOperation.CompleteDeleteOperation forRankField(Long var1);
    }

    private static class Builder implements LexoRankDeleteOperation.ForIssueOrRankField, LexoRankDeleteOperation.CompleteDeleteOperation {
        private LexoRankDao lexoRankDao;
        private LexoRankStatisticsAgent lexoRankStatisticsAgent;
        private LexoRankDeleteOperation.DeleteOperationType deleteOperationType;
        private Long objectTypeId;

        private Builder(LexoRankDao lexoRankDao, LexoRankStatisticsAgent lexoRankStatisticsAgent) {
            this.lexoRankDao = lexoRankDao;
            this.lexoRankStatisticsAgent = lexoRankStatisticsAgent;
        }

        @Override
        public LexoRankDeleteOperation.CompleteDeleteOperation forIssue(Long issueId) {
            this.deleteOperationType = LexoRankDeleteOperation.DeleteOperationType.ISSUE;
            this.objectTypeId = issueId;
            return this;
        }

        @Override
        public LexoRankDeleteOperation.CompleteDeleteOperation forRankField(Long rankFieldId) {
            this.deleteOperationType = LexoRankDeleteOperation.DeleteOperationType.RANK_FIELD;
            this.objectTypeId = rankFieldId;
            return this;
        }

        @Override
        public LexoRankDeleteOperation build() {
            return new LexoRankDeleteOperation(this.lexoRankDao, this.lexoRankStatisticsAgent, this.deleteOperationType, this.objectTypeId);
        }
    }

    private static enum DeleteOperationType {
        ISSUE,
        RANK_FIELD;

        private DeleteOperationType() {
        }
    }
}
