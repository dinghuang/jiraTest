package com.example.demo.test;


/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
public class LexoRankChangeEvent {

    private final String newRank;
    private final Integer oldBucket;
    private final Integer newBucket;
    private final Integer oldRankLength;
    private final Integer newRankLength;
    private final Long fieldId;
    private final Long issueId;

    public LexoRankChangeEvent(LexoRankChange lexoRankChange) {
        this.fieldId = lexoRankChange.getCustomFieldId();
        this.issueId = lexoRankChange.getIssueId();
        if (lexoRankChange.getOldRank() != null) {
            this.oldBucket = lexoRankChange.getOldRank().getBucket().intValue();
            this.oldRankLength = lexoRankChange.getOldRank().format().length();
        } else {
            this.oldBucket = null;
            this.oldRankLength = null;
        }

        if (lexoRankChange.getNewRank() != null) {
            this.newRank = lexoRankChange.getNewRank().format();
            this.newBucket = lexoRankChange.getNewRank().getBucket().intValue();
            this.newRankLength = this.newRank.length();
        } else {
            this.newRank = null;
            this.newBucket = null;
            this.newRankLength = null;
        }

    }

    public LexoRankChangeEvent(Long fieldId, Long issueId, String newRank) {
        this.newRank = newRank;
        this.oldBucket = null;
        this.newBucket = null;
        this.oldRankLength = null;
        this.newRankLength = newRank.length();
        this.fieldId = fieldId;
        this.issueId = issueId;
    }

    public String getNewRank() {
        return this.newRank;
    }

    public Integer getOldBucket() {
        return this.oldBucket;
    }

    public Integer getNewBucket() {
        return this.newBucket;
    }

    public Integer getOldRankLength() {
        return this.oldRankLength;
    }

    public Integer getNewRankLength() {
        return this.newRankLength;
    }

    public Long getFieldId() {
        return this.fieldId;
    }

    public Long getIssueId() {
        return this.issueId;
    }
}
