package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.chart.*;
import com.atlassian.greenhopper.web.rapid.issue.StatisticFieldHelper;
import com.atlassian.greenhopper.web.rapid.issue.StatusEntryFactory;
import com.atlassian.greenhopper.web.rapid.issue.statistics.HistoricalEstimateStatisticValueResolver;
import com.atlassian.greenhopper.web.rapid.issue.statistics.IssueSprintStatistics;
import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Iterables;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
public class HistoricSprintReportContentsFactory {
    private final List<RapidIssueEntry> issuesList;
    private final HistoricalEstimateStatisticValueResolver historicalEstimateStatisticValueResolver;
    private final IssueService issueService;
    private final ApplicationUser loggedInUser;
    private final StatisticFieldHelper statisticFieldHelper;
    private final StatusManager statusManager;
    private final StatusEntryFactory statusEntryFactory;

    public HistoricSprintReportContentsFactory(List<RapidIssueEntry> issuesList, HistoricalEstimateStatisticValueResolver historicalEstimateStatisticValueResolver, IssueService issueService, ApplicationUser loggedInUser, StatisticFieldHelper statisticFieldHelper, StatusManager statusManager, StatusEntryFactory statusEntryFactory) {
        this.issuesList = issuesList;
        this.historicalEstimateStatisticValueResolver = historicalEstimateStatisticValueResolver;
        this.issueService = issueService;
        this.loggedInUser = loggedInUser;
        this.statisticFieldHelper = statisticFieldHelper;
        this.statusManager = statusManager;
        this.statusEntryFactory = statusEntryFactory;
    }

    public HistoricSprintReportContents create() {
        //问题估计计算器
        List<? extends IssueEstimateCalculator> endStateCalculators = this.createEndStateCalculators();
        IssueEstimateCalculator addedDuringCalculator = new AddedDuringIssueEstimateCalculator();
        Iterator var3 = this.issuesList.iterator();

        while(true) {
            RapidIssueEntry issueEntry;
            IssueSprintStatistics issueSprintStatistics;
            IssueService.IssueResult issueResult;
            do {
                do {
                    do {
                        if(!var3.hasNext()) {
                            HistoricSprintReportContents contents = new HistoricSprintReportContents();
                            EstimateValue allIssuesEstimateSum = new EstimateValue();
                            Iterable<IssueEstimateCalculator> allCalculators = Iterables.concat(endStateCalculators, Collections.singleton(addedDuringCalculator));
                            Iterator var12 = allCalculators.iterator();

                            while(var12.hasNext()) {
                                IssueEstimateCalculator calculator = (IssueEstimateCalculator)var12.next();
                                calculator.fillIntoReportContents(contents);
                                allIssuesEstimateSum.add(calculator.getCurrentEstimateSum());
                            }

                            contents.allIssuesEstimateSum = new RapidIssueEntry.NumberFieldValue(allIssuesEstimateSum.get());
                            return contents;
                        }

                        issueEntry = (RapidIssueEntry)var3.next();
                        issueSprintStatistics = this.historicalEstimateStatisticValueResolver.getIssueSprintStatistics(issueEntry.key);
                    } while(issueSprintStatistics == null);
                } while(issueSprintStatistics.issueStatusAtEndOfSprint == null);

                issueResult = this.issueService.getIssue(this.loggedInUser, issueEntry.key);
            } while(!issueResult.isValid());

            this.prepareIssueEntryForCalculation(issueEntry, issueSprintStatistics, issueResult);
            Iterator var7 = endStateCalculators.iterator();

            while(var7.hasNext()) {
                IssueEstimateCalculator calculator = (IssueEstimateCalculator)var7.next();
                if(calculator.canHandleIssue(issueSprintStatistics)) {
                    calculator.add(issueEntry);
                    break;
                }
            }

            if(addedDuringCalculator.canHandleIssue(issueSprintStatistics)) {
                addedDuringCalculator.add(issueEntry);
            }
        }
    }

    private List<? extends IssueEstimateCalculator> createEndStateCalculators() {
        return Arrays.asList(new CompletedIssueEstimateCalculator(), new RemovedIssueEstimateCalculator(), new CompletedInAnotherSprintIssueEstimateCalculator(), new NotCompletedIssueEstimateCalculator());
    }

    private void prepareIssueEntryForCalculation(RapidIssueEntry issueEntry, IssueSprintStatistics issueSprintStatistics, IssueService.IssueResult issueResult) {
        issueEntry.estimateStatistic = this.statisticFieldHelper.createEstimateStatistic(issueResult.getIssue(), this.historicalEstimateStatisticValueResolver);
        issueEntry.currentEstimateStatistic = this.statisticFieldHelper.createCurrentEstimateStatistic(issueResult.getIssue(), this.historicalEstimateStatisticValueResolver);
        Status status = this.statusManager.getStatus(issueSprintStatistics.issueStatusAtEndOfSprint);
        issueEntry.setStatusDetails(status, this.statusEntryFactory.createStatusEntry(status));
    }
}
