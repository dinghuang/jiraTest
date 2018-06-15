package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import javax.annotation.Nonnull;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
class RemovedIssueEstimateCalculator extends AbstractIssueEstimateCalculator implements IssueEstimateCalculator {
    RemovedIssueEstimateCalculator() {
    }

    @Override
    public boolean canHandleIssue(@Nonnull IssueSprintStatistics issueSprintStatistics) {
        return issueSprintStatistics.wasIssueRemovedDuringSprint;
    }

    @Override
    public void fillIntoReportContents(@Nonnull HistoricSprintReportContents contents) {
        contents.puntedIssues = this.getIssueEntries();
        contents.puntedIssuesEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getCurrentEstimateSum());
        contents.puntedIssuesInitialEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getInitialEstimateSum());
    }
}
