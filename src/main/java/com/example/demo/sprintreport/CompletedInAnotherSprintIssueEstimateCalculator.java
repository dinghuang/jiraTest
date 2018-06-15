package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import javax.annotation.Nonnull;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
class CompletedInAnotherSprintIssueEstimateCalculator extends AbstractIssueEstimateCalculator implements IssueEstimateCalculator {
    CompletedInAnotherSprintIssueEstimateCalculator() {
    }

    @Override
    public boolean canHandleIssue(@Nonnull IssueSprintStatistics issueSprintStatistics) {
        return issueSprintStatistics.wasIssueCompleted && !issueSprintStatistics.wasIssueCompletedInTheCurrentSprint;
    }

    @Override
    public void fillIntoReportContents(@Nonnull HistoricSprintReportContents contents) {
        contents.issuesCompletedInAnotherSprint = this.getIssueEntries();
        contents.issuesCompletedInAnotherSprintEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getCurrentEstimateSum());
        contents.issuesCompletedInAnotherSprintInitialEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getInitialEstimateSum());
    }
}

