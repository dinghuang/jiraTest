package com.example.demo.test;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoRankBalanceChange {
    private final LexoRankBalanceOperation.BalanceOperationType operationType;
    private final Long rankFieldId;
    private final Long issueId;
    private final LexoRankBucket oldBucket;
    private final LexoRankBucket newBucket;
    private final LexoRank oldRank;
    private final LexoRank newRank;

    private LexoRankBalanceChange(Long rankFieldId, LexoRankBalanceOperation.BalanceOperationType operationType, Long issueId, LexoRankBucket oldBucket, LexoRankBucket newBucket, LexoRank oldRank, LexoRank newRank) {
        this.rankFieldId = rankFieldId;
        this.operationType = operationType;
        this.issueId = issueId;
        this.oldBucket = oldBucket;
        this.newBucket = newBucket;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public static LexoRankBalanceChange.ForRankField builder() {
        return new LexoRankBalanceChange.Builder();
    }

    public LexoRankBalanceOperation.BalanceOperationType getOperationType() {
        return this.operationType;
    }

    public Long getRankFieldId() {
        return this.rankFieldId;
    }

    public Long getIssueId() {
        return this.issueId;
    }

    public boolean isVirtualIssue() {
        return this.issueId != null && this.issueId.longValue() < 0L;
    }

    public LexoRankBucket getOldBucket() {
        return this.oldBucket;
    }

    public LexoRankBucket getNewBucket() {
        return this.newBucket;
    }

    public LexoRank getOldRank() {
        return this.oldRank;
    }

    public LexoRank getNewRank() {
        return this.newRank;
    }

    public interface CompleteBalanceChangeEvent {
        LexoRankBalanceChange build();
    }

    public interface ChangedRankTo {
        LexoRankBalanceChange.CompleteBalanceChangeEvent changedRankTo(LexoRank var1);
    }

    public interface ChangedRankFrom {
        LexoRankBalanceChange.ChangedRankTo changedRankFrom(LexoRank var1);
    }

    public interface MovedToBucket {
        LexoRankBalanceChange.ChangedRankFrom movedToBucket(LexoRankBucket var1);
    }

    public interface MovedFromBucket {
        LexoRankBalanceChange.MovedToBucket movedFromBucket(LexoRankBucket var1);
    }

    public interface ForIssue {
        LexoRankBalanceChange.MovedFromBucket forIssue(Long var1);
    }

    public interface PerformedBalanceOperation {
        LexoRankBalanceChange.MovedFromBucket moveMarkerRow(LexoRankBalanceOperation.BalanceOperationType var1);

        LexoRankBalanceChange.MovedFromBucket movedMaximumMarkerRow();

        LexoRankBalanceChange.MovedFromBucket movedMinimumMarkerRow();

        LexoRankBalanceChange.ForIssue movedIssueRow();
    }

    public interface ForRankField {
        LexoRankBalanceChange.PerformedBalanceOperation forRankField(Long var1);
    }

    private static class Builder implements LexoRankBalanceChange.ForRankField, LexoRankBalanceChange.PerformedBalanceOperation, LexoRankBalanceChange.ForIssue, LexoRankBalanceChange.MovedFromBucket, LexoRankBalanceChange.MovedToBucket, LexoRankBalanceChange.ChangedRankFrom, LexoRankBalanceChange.ChangedRankTo, LexoRankBalanceChange.CompleteBalanceChangeEvent {
        private LexoRankBalanceOperation.BalanceOperationType operationType;
        private Long rankFieldId;
        private Long issueId;
        private LexoRankBucket oldBucket;
        private LexoRankBucket newBucket;
        private LexoRank oldRank;
        private LexoRank newRank;

        private Builder() {
        }

        @Override
        public LexoRankBalanceChange.ChangedRankTo changedRankFrom(LexoRank rank) {
            this.oldRank = rank;
            return this;
        }

        @Override
        public LexoRankBalanceChange.CompleteBalanceChangeEvent changedRankTo(LexoRank rank) {
            this.newRank = rank;
            return this;
        }

        @Override
        public LexoRankBalanceChange build() {
            return new LexoRankBalanceChange(this.rankFieldId, this.operationType, this.issueId, this.oldBucket, this.newBucket, this.oldRank, this.newRank);
        }

        @Override
        public LexoRankBalanceChange.MovedFromBucket forIssue(Long issueId) {
            this.issueId = issueId;
            return this;
        }

        @Override
        public LexoRankBalanceChange.PerformedBalanceOperation forRankField(Long rankFieldId) {
            this.rankFieldId = rankFieldId;
            return this;
        }

        @Override
        public LexoRankBalanceChange.MovedToBucket movedFromBucket(LexoRankBucket bucket) {
            this.oldBucket = bucket;
            return this;
        }

        @Override
        public LexoRankBalanceChange.ChangedRankFrom movedToBucket(LexoRankBucket bucket) {
            this.newBucket = bucket;
            return this;
        }

        @Override
        public LexoRankBalanceChange.MovedFromBucket movedMaximumMarkerRow() {
            this.operationType = LexoRankBalanceOperation.BalanceOperationType.MOVE_MAX;
            return this;
        }

        @Override
        public LexoRankBalanceChange.MovedFromBucket movedMinimumMarkerRow() {
            this.operationType = LexoRankBalanceOperation.BalanceOperationType.MOVE_MAX;
            return this;
        }

        @Override
        public LexoRankBalanceChange.MovedFromBucket moveMarkerRow(LexoRankBalanceOperation.BalanceOperationType operationType) {
            switch (operationType.ordinal()) {
                case 1:
                    return this.movedMaximumMarkerRow();
                case 2:
                    return this.movedMinimumMarkerRow();
                default:
                    throw new IllegalArgumentException("Only balance operation types MOVE_MAX and MOVE_MIN can be used.");
            }
        }

        @Override
        public LexoRankBalanceChange.ForIssue movedIssueRow() {
            this.operationType = LexoRankBalanceOperation.BalanceOperationType.MOVE_NEXT;
            return this;
        }
    }

}
