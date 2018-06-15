package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import javax.annotation.Nonnull;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
class NotCompletedIssueEstimateCalculator extends AbstractIssueEstimateCalculator implements IssueEstimateCalculator {
    NotCompletedIssueEstimateCalculator() {
    }

    @Override
    public boolean canHandleIssue(@Nonnull IssueSprintStatistics issueSprintStatistics) {
        return !issueSprintStatistics.wasIssueCompletedInTheCurrentSprint && !issueSprintStatistics.wasIssueCompleted && !issueSprintStatistics.wasIssueRemovedDuringSprint;
    }
    @Override
    public void fillIntoReportContents(@Nonnull HistoricSprintReportContents contents) {
        contents.issuesNotCompletedInCurrentSprint = this.getIssueEntries();
        contents.issuesNotCompletedEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getCurrentEstimateSum());
        contents.issuesNotCompletedInitialEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getInitialEstimateSum());
    }
}
