package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.chart.EstimateValue;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
abstract class AbstractIssueEstimateCalculator implements IssueEstimateCalculator {
    private final List<RapidIssueEntry> issueEntries = Lists.newArrayList();
    private final EstimateValue currentEstimateSum = new EstimateValue();
    private final EstimateValue initialEstimateSum = new EstimateValue();

    AbstractIssueEstimateCalculator() {
    }

    @Override
    public final void add(@Nonnull RapidIssueEntry issueEntry) {
        this.issueEntries.add(issueEntry);
        this.currentEstimateSum.add(this.getStatValue(issueEntry.getCurrentEstimateStatistic()));
        this.initialEstimateSum.add(this.getStatValue(issueEntry.getEstimateStatistic()));
    }

    @Override
    public final Double getCurrentEstimateSum() {
        return this.currentEstimateSum.get();
    }

    protected final Double getInitialEstimateSum() {
        return this.initialEstimateSum.get();
    }

    protected final List<RapidIssueEntry> getIssueEntries() {
        return this.issueEntries;
    }

    private Double getStatValue(RapidIssueEntry.StatisticFieldValue fieldValue) {
        return fieldValue != null?fieldValue.getStatFieldValue().getValue():null;
    }
}
