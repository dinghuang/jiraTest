package com.example.demo.test;


/**
 * 排名变化
 *
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoRankChange implements RankChange {
    private final long customFieldId;
    private final long issueId;
    private final LexoRank oldRank;
    private final LexoRank newRank;

    private LexoRankChange(long customFieldId, long issueId, LexoRank oldRank, LexoRank newRank) {
        this.customFieldId = customFieldId;
        this.issueId = issueId;
        this.oldRank = oldRank;
        this.newRank = newRank;
    }

    public static LexoRankChange.ForRankField builder() {
        return new LexoRankChange.Builder();
    }

    @Override
    public boolean wasChanged() {
        return this.oldRank == null && this.newRank != null || this.oldRank != null && !this.oldRank.equals(this.newRank);
    }

    @Override
    public long getCustomFieldId() {
        return this.customFieldId;
    }

    @Override
    public long getIssueId() {
        return this.issueId;
    }

    public LexoRank getOldRank() {
        return this.oldRank;
    }

    public LexoRank getNewRank() {
        return this.newRank;
    }

    @Override
    public String toString() {
        return "LexoRankChange(customFieldId=" + this.customFieldId + ",issueId=" + this.issueId + ",oldRank" + this.oldRank + ",newRank=" + this.newRank + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            LexoRankChange that = (LexoRankChange) o;
            return this.customFieldId == that.customFieldId && (this.issueId == that.issueId && (this.oldRank.equals(that.oldRank) && this.newRank.equals(that.newRank)));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = (int) (this.customFieldId ^ this.customFieldId >>> 32);
        result = 31 * result + (int) (this.issueId ^ this.issueId >>> 32);
        result = 31 * result + this.oldRank.hashCode();
        result = 31 * result + this.newRank.hashCode();
        return result;
    }

    public interface CompleteLexoRankChange {
        LexoRankChange build();
    }

    public interface RankedTo {
        LexoRankChange.CompleteLexoRankChange to(LexoRank var1);
    }

    public interface NoRankChange {
        LexoRankChange.CompleteLexoRankChange noop(LexoRank var1);
    }

    public interface RankedInitially {
        LexoRankChange.CompleteLexoRankChange initially(LexoRank var1);
    }

    public interface RankedFrom extends LexoRankChange.RankedInitially, LexoRankChange.NoRankChange {
        LexoRankChange.RankedTo from(LexoRank var1);
    }

    public interface RankedIssue {
        LexoRankChange.RankedFrom rankedIssue(Long var1);
    }

    public interface ForRankField {
        LexoRankChange.RankedIssue forRankField(Long var1);
    }

    private static class Builder implements LexoRankChange.ForRankField, LexoRankChange.RankedIssue, LexoRankChange.RankedFrom, LexoRankChange.RankedTo, LexoRankChange.CompleteLexoRankChange {
        private long rankFieldId;
        private long issueId;
        private LexoRank oldRank;
        private LexoRank newRank;

        private Builder() {
        }

        @Override
        public LexoRankChange build() {
            return new LexoRankChange(this.rankFieldId, this.issueId, this.oldRank, this.newRank);
        }

        @Override
        public LexoRankChange.RankedIssue forRankField(Long rankFieldId) {
            this.rankFieldId = rankFieldId;
            return this;
        }

        @Override
        public LexoRankChange.RankedTo from(LexoRank oldRank) {
            this.oldRank = oldRank;
            return this;
        }

        @Override
        public LexoRankChange.CompleteLexoRankChange initially(LexoRank newRank) {
            this.oldRank = null;
            this.newRank = newRank;
            return this;
        }

        @Override
        public LexoRankChange.CompleteLexoRankChange noop(LexoRank existingRank) {
            this.oldRank = existingRank;
            this.newRank = existingRank;
            return this;
        }

        @Override
        public LexoRankChange.RankedFrom rankedIssue(Long issueId) {
            this.issueId = issueId;
            return this;
        }

        @Override
        public LexoRankChange.CompleteLexoRankChange to(LexoRank newRank) {
            this.newRank = newRank;
            return this;
        }
    }
}
