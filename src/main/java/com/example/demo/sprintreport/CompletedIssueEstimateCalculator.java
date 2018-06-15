package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import javax.annotation.Nonnull;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
class CompletedIssueEstimateCalculator extends AbstractIssueEstimateCalculator implements IssueEstimateCalculator {
    CompletedIssueEstimateCalculator() {
    }

    @Override
    public boolean canHandleIssue(@Nonnull IssueSprintStatistics issueSprintStatistics) {
        return issueSprintStatistics.wasIssueCompletedInTheCurrentSprint;
    }

    @Override
    public void fillIntoReportContents(@Nonnull HistoricSprintReportContents contents) {
        contents.completedIssues = this.getIssueEntries();
        contents.completedIssuesEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getCurrentEstimateSum());
        contents.completedIssuesInitialEstimateSum = new RapidIssueEntry.NumberFieldValue(this.getInitialEstimateSum());
    }
}

