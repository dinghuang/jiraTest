package com.example.demo.test;

import com.atlassian.greenhopper.global.LoggerWrapper;
import com.atlassian.greenhopper.manager.lexorank.balancer.BalancerEntry;
import com.atlassian.greenhopper.manager.lexorank.balancer.BalancerEntryManager;
import com.atlassian.greenhopper.model.validation.ErrorCollectionTransformer;
import com.atlassian.greenhopper.service.ServiceResult;
import com.atlassian.greenhopper.service.lexorank.balance.*;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
@Service
public class LexoRankScheduledBalanceHandler implements Runnable {
    protected final LoggerWrapper log = LoggerWrapper.with(this.getClass());
    @Autowired
    private BalancerEntryManager balancerEntryManager;
    @Autowired
    private com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalancer lexoRankBalancer;
    @Autowired
    private ErrorCollectionTransformer errorCollectionTransformer;
    private volatile boolean isRunning;

    public LexoRankScheduledBalanceHandler() {
    }

    @Override
    public void run() {
        try {
            this.log.debug("Executing LexoRank scheduled balance", new Object[0]);
            this.isRunning = true;
            List<BalancerEntry> entriesToBalance = this.balancerEntryManager.list();
            this.log.debug("Balancer table contains %d balancer entries", new Object[]{Integer.valueOf(entriesToBalance.size())});
            Iterator var2 = entriesToBalance.iterator();

            while (var2.hasNext()) {
                BalancerEntry balancerEntry = (BalancerEntry) var2.next();
                DateTime rebalanceTime = balancerEntry.getRebalanceTime();
                if (this.log.isDebugEnabled()) {
                    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");
                    this.log.debug("Balancer entry fieldId=%d rebalanceTime=%s", new Object[]{balancerEntry.getFieldId(), formatter.print(rebalanceTime)});
                }

                if (!rebalanceTime.isBeforeNow() && !rebalanceTime.isEqualNow()) {
                    this.log.debug("Balance NOT required for fieldId=%d", new Object[]{balancerEntry.getFieldId()});
                } else {
                    this.log.debug("Balancing fieldId=%d", new Object[]{balancerEntry.getFieldId()});
                    ServiceResult balanceFieldsOutcome = this.lexoRankBalancer.balanceFieldIds(Lists.newArrayList(new Long[]{balancerEntry.getFieldId()}));
                    if (balanceFieldsOutcome.isValid()) {
                        this.log.debug("balance for fieldId=%d succeeded, removing balancer entry", new Object[]{balancerEntry.getFieldId()});
                        this.balancerEntryManager.delete(balancerEntry);
                    } else {
                        this.log.warn("Balance for fieldId=%d returned errors: %s", new Object[]{balancerEntry.getFieldId(), this.errorCollectionTransformer.toJiraErrorCollection(balanceFieldsOutcome.getErrors())});
                    }
                }
            }
        } finally {
            this.isRunning = false;
            this.log.debug("LexoRank scheduled balance finished", new Object[0]);
        }

    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
