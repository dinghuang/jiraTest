package com.example.demo.sprintreport;

import com.atlassian.greenhopper.web.rapid.list.RapidIssueEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史性的Sprint报告内容
 *
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
public class HistoricSprintReportContents extends RestTemplate {

    /**
     * 完成问题
     */
    public List<RapidIssueEntry> completedIssues = new ArrayList();
    /**
     * 当前Sprint中未完成的问题
     */
    public List<RapidIssueEntry> issuesNotCompletedInCurrentSprint = new ArrayList();
    /**
     * 从冲刺removed的问题
     */
    public List<RapidIssueEntry> puntedIssues = new ArrayList();
    /**
     * 在另一个Sprint中完成的问题。
     */
    public List<RapidIssueEntry> issuesCompletedInAnotherSprint = new ArrayList();
    /**
     * 完成issue初始估算时间总和
     */
    public RapidIssueEntry.NumberFieldValue completedIssuesInitialEstimateSum;
    /**
     * 完成issue估算时间总和
     */
    public RapidIssueEntry.NumberFieldValue completedIssuesEstimateSum;
    /**
     * 未完成issue初始估算时间总和
     */
    public RapidIssueEntry.NumberFieldValue issuesNotCompletedInitialEstimateSum;
    /**
     * 完成issue初始估算时间总和
     */
    public RapidIssueEntry.NumberFieldValue issuesNotCompletedEstimateSum;
    /**
     * 所有问题估计和
     */
    public RapidIssueEntry.NumberFieldValue allIssuesEstimateSum;
    /**
     * 移除问题初始估计时间总和
     */
    public RapidIssueEntry.NumberFieldValue puntedIssuesInitialEstimateSum;
    /**
     * 移除问题估计时间总和
     */
    public RapidIssueEntry.NumberFieldValue puntedIssuesEstimateSum;
    /**
     * 在另一个冲刺完成的问题初始估计时间总和
     */
    public RapidIssueEntry.NumberFieldValue issuesCompletedInAnotherSprintInitialEstimateSum;
    /**
     * 在另一个冲刺完成的问题估计时间总和
     */
    public RapidIssueEntry.NumberFieldValue issuesCompletedInAnotherSprintEstimateSum;
    /**
     * 冲刺运行期间添加的issue
     */
    public Map<String, Boolean> issueKeysAddedDuringSprint = new HashMap();

    public HistoricSprintReportContents() {
    }
}