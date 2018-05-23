package com.example.demo.test;

import com.atlassian.greenhopper.global.LoggerWrapper;
import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobHandlerKey;
import com.atlassian.scheduler.compat.JobInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 排名平衡job执行
 *
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
@Service
public class LexoRankBalancePluginJob implements JobHandler {

    protected final LoggerWrapper log = LoggerWrapper.with(this.getClass());

    @Autowired
    private LexoRankBalancingService lexoRankBalancingService;
    public static JobHandlerKey JOB_HANDLER_KEY = JobHandlerKey.of(LexoRankBalancePluginJob.class.getName());

    public LexoRankBalancePluginJob() {
    }

    @Override
    public void execute(JobInfo jobInfo) {
        this.log.debug("Scheduler job submitting a scheduled balance request", new Object[0]);
        this.lexoRankBalancingService.submitScheduledBalance();
    }
}
