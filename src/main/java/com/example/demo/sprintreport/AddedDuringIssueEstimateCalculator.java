package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
class AddedDuringIssueEstimateCalculator extends AbstractIssueEstimateCalculator implements IssueEstimateCalculator {

    AddedDuringIssueEstimateCalculator() {
    }

    @Override
    public boolean canHandleIssue(@Nonnull IssueSprintStatistics issueSprintStatistics) {
        return issueSprintStatistics.wasIssueAddedDuringSprint;
    }

    @Override
    public void fillIntoReportContents(@Nonnull HistoricSprintReportContents contents) {
        Iterator var2 = this.getIssueEntries().iterator();

        while (var2.hasNext()) {
            RapidIssueEntry issueEntry = (RapidIssueEntry) var2.next();
            contents.issueKeysAddedDuringSprint.put(issueEntry.key, Boolean.TRUE);
        }

    }
}
