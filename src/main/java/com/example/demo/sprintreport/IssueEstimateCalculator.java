package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
interface IssueEstimateCalculator {
    boolean canHandleIssue(@Nonnull IssueSprintStatistics var1);

    void add(@Nonnull RapidIssueEntry var1);

    void fillIntoReportContents(@Nonnull HistoricSprintReportContents var1);

    @Nullable
    Double getCurrentEstimateSum();
}
