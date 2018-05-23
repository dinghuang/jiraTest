package com.example.demo.test;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.greenhopper.global.LoggerWrapper;
import com.atlassian.greenhopper.manager.lexorank.balancer.BalancerEntry;
import com.atlassian.greenhopper.manager.lexorank.balancer.BalancerEntryManager;
import com.atlassian.greenhopper.manager.lexorank.suspend.LexoRankSuspendManager;
import com.atlassian.greenhopper.model.validation.ErrorCollection;
import com.atlassian.greenhopper.service.ServiceOutcome;
import com.atlassian.greenhopper.service.ServiceOutcomeImpl;
import com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalancePluginJob;
import com.atlassian.jira.cluster.ClusterManager;
import com.atlassian.jira.cluster.Node;
import com.atlassian.jira.config.ForegroundIndexTaskContext;
import com.atlassian.jira.config.properties.JiraProperties;
import com.atlassian.jira.index.ha.OfBizNodeIndexCounterStore;
import com.atlassian.jira.index.ha.ReplicatedIndexOperation;
import com.atlassian.jira.index.ha.ReplicatedIndexOperationFactory;
import com.atlassian.jira.issue.index.ReindexAllCompletedEvent;
import com.atlassian.jira.issue.index.ReindexAllStartedEvent;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.ofbiz.OfBizListIterator;
import com.atlassian.jira.task.TaskDescriptor;
import com.atlassian.jira.task.TaskManager;
import com.atlassian.plugin.event.events.PluginFrameworkShuttingDownEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.sal.api.component.ComponentLocator;
import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.joda.time.DateTime;
import org.ofbiz.core.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
@Service
public class LexoRankBalancingService {
    private final LoggerWrapper log = LoggerWrapper.with(this.getClass());
    private static final String LEXO_RANK_SCHEDULER_JOB = "LEXO_RANK_SCHEDULER_JOB";
    private static final long SCHEDULER_JOB_REPEAT_INTERVAL = 60000L;
    static final int MIN_RANK_LENGTH_FOR_IMMEDIATE_REBALANCE = 100;
    static final int MIN_RANK_LENGTH_FOR_REBALANCE = 50;
    private static final int HOURS_12 = 12;
    private static final String GH_SHUTDOWN_TRIGGER_KEY = "com.pyxis.greenhopper.jira:sprint-remote-link-aggregator";
    private static final String JIRA_AGILE_LEXORANK_BALANCING_BACKOFF_THRESHOLD = "jira.agile.lexorank.balancing.backoff.threshold";
    private static final long DEFAULT_LEXORANK_BALANCING_BACKOFF_THRESHOLD_IN_MILLIS = 30000L;
    @Autowired
    private LexoRankDao lexoRankDao;
    @Autowired
    private BalancerEntryManager balancerEntryManager;
    @Autowired
    private CompatibilityPluginScheduler compatibilityPluginScheduler;
    private final ThreadPoolExecutor executorService = new LexoRankBalancingService.LexoRankExecutor();
    @Autowired
    private LexoRankScheduledBalanceHandler lexoRankScheduledBalanceHandler;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalancePluginJob lexoRankBalancePluginJob;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private OfBizDelegator ofBizDelegator;
    @Autowired
    private ClusterManager clusterManager;
    @Autowired
    private JiraProperties jiraProperties;
    @Autowired
    private LexoRankSuspendManager lexoRankSuspendManager;
    private static final ReplicatedIndexOperationFactory operationFactory = new ReplicatedIndexOperationFactory();
    private final AtomicBoolean isServiceInitialised = new AtomicBoolean(false);
    private final AtomicBoolean isBalancingDisabled = new AtomicBoolean(true);
    private final AtomicBoolean isServiceShutdown = new AtomicBoolean(false);

    public LexoRankBalancingService() {
    }

    @PostConstruct
    public void onSpringContextStarted() {
        this.eventPublisher.register(this);
    }

    @PreDestroy
    public void onSpringContextStopped() {
        this.eventPublisher.unregister(this);
    }

    public void initialise() {
        if (this.isServiceInitialised.compareAndSet(false, true)) {
            this.log.info("Initialising LexoRank Balancing Service", new Object[0]);
            this.compatibilityPluginScheduler.registerJobHandler(com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalancePluginJob.JOB_HANDLER_KEY, this.lexoRankBalancePluginJob);
            if (this.compatibilityPluginScheduler.getJobInfo("LEXO_RANK_SCHEDULER_JOB") == null) {
                this.log.info("Scheduling clustered job, jobKey=%s, JobHandlerKey=%s", new Object[]{"LEXO_RANK_SCHEDULER_JOB", com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalancePluginJob.JOB_HANDLER_KEY});
                this.compatibilityPluginScheduler.scheduleClusteredJob("LEXO_RANK_SCHEDULER_JOB", com.atlassian.greenhopper.service.lexorank.balance.LexoRankBalancePluginJob.JOB_HANDLER_KEY, new Date(), 60000L);
            } else {
                this.log.info("Scheduler job already present in db, not scheduling again", new Object[0]);
            }

            boolean foregroundIndexRunning = this.isForegroundReindexRunning();
            this.isBalancingDisabled.set(foregroundIndexRunning);
            this.log.info("LexoRank Balancing Service is initialised, foregroundIndexRunning=" + foregroundIndexRunning, new Object[0]);
        }

    }

    @EventListener
    public void pluginFrameworkShuttingDown(PluginFrameworkShuttingDownEvent evt) {
        this.shutdown();
    }

    @EventListener
    public void pluginModuleDisabled(PluginModuleDisabledEvent evt) {
        if (evt.getModule().getCompleteKey().equals("com.pyxis.greenhopper.jira:sprint-remote-link-aggregator")) {
            this.shutdown();
        }

    }

    @EventListener
    public void onJiraReindexStart(ReindexAllStartedEvent event) {
        if (!event.isUsingBackgroundIndexing()) {
            this.disableBalancing();
        }

    }

    @EventListener
    public void onJiraReindexComplete(ReindexAllCompletedEvent event) {
        if (!event.isUsingBackgroundIndexing()) {
            this.enableBalancing();
        }

    }

    public boolean isBalancingDisabled() {
        return this.isBalancingDisabled.get();
    }

    public boolean shouldBalancingBackOff() {
        //这一步是从缓冲中拿值
        long backoffThreshold = this.jiraProperties.getLong("jira.agile.lexorank.balancing.backoff.threshold", 30000L);
        if (backoffThreshold < 0L) {
            this.log.debug("LexoRank backoff is disabled since jira.agile.lexorank.balancing.backoff.threshold property is set to negative value.", new Object[0]);
            return false;
        } else if (this.getMaxIndexingDelayForLiveNodes() > backoffThreshold) {
            //节点最大索引延迟大于缓冲中的
            this.log.debug("For at least one node index replication is behind" +
                    " current node for more than threshold=%s seconds. Balancing is terminating" +
                    ". It will resume once index replication lag for all nodes will be within a threshold.", new Object[]{Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(backoffThreshold))});
            return true;
        } else {
            return false;
        }
    }

    private long getMaxIndexingDelayForLiveNodes() {
        String currentNode = this.clusterManager.getNodeId();
        long maxDelay = 0L;
        if (currentNode != null) {
            Iterator var4 = this.clusterManager.findLiveNodes().iterator();

            while (var4.hasNext()) {
                Node node = (Node) var4.next();
                if (!currentNode.equals(node.getNodeId())) {
                    maxDelay = Math.max(this.getDelayBetweenNodes(currentNode, node.getNodeId()), maxDelay);
                    maxDelay = Math.max(this.getDelayBetweenNodes(node.getNodeId(), currentNode), maxDelay);
                }
            }
        }

        return maxDelay;
    }

    private long getDelayBetweenNodes(String sendingNodeId, String receivingNodeId) {
        long now = System.currentTimeMillis();
        long currentIndexCount = this.getCurrentIndexCount(receivingNodeId, sendingNodeId);
        ReplicatedIndexOperation indexOp = this.getFirstIndexOperationAfter(sendingNodeId, Long.valueOf(currentIndexCount));
        if (indexOp != null) {
            long delay = Math.max(0L, now - indexOp.getIndexTime().getTime());
            this.log.debug("Index replication on node %s is behind node %s for %s seconds. (Based on replicated operation id: %s)", new Object[]{receivingNodeId, sendingNodeId, Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(delay)), Long.valueOf(indexOp.getId())});
            return delay;
        } else {
            return 0L;
        }
    }

    private long getCurrentIndexCount(String receivingNodeId, String sendingNodeId) {
        OfBizNodeIndexCounterStore ofBizNodeIndexCounterStore = (OfBizNodeIndexCounterStore) ComponentLocator.getComponent(OfBizNodeIndexCounterStore.class);
        return ofBizNodeIndexCounterStore.getIndexOperationCounterForNodeId(receivingNodeId, sendingNodeId);
    }

    private ReplicatedIndexOperation getFirstIndexOperationAfter(String sourceNodeId, Long id) {
        ImmutableList<EntityCondition> entityConditions = ImmutableList.of(new EntityExpr("nodeId", EntityOperator.EQUALS, sourceNodeId), new EntityExpr("id", EntityOperator.GREATER_THAN, id));
        EntityConditionList entityConditionList = new EntityConditionList(entityConditions, EntityOperator.AND);
        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setMaxResults(1);
        OfBizListIterator gvs = this.ofBizDelegator.findListIteratorByCondition("ReplicatedIndexOperation", entityConditionList, (EntityCondition) null, (Collection) null, ImmutableList.of("indexTime"), findOptions);

        try {
            Iterator var7 = gvs.iterator();
            if (var7.hasNext()) {
                GenericValue gv = (GenericValue) var7.next();
                ReplicatedIndexOperation var9 = operationFactory.build(gv);
                return var9;
            }
        } finally {
            gvs.close();
        }

        return null;
    }

    @EventListener
    public void onLexoRankEvent(com.atlassian.greenhopper.service.lexorank.balance.LexoRankChangeEvent event) {
        String rank = event.getNewRank();
        Long fieldId = event.getFieldId();
        this.log.debug("received LexoRankBalanceEvent fieldId=%d rank=%s", new Object[]{fieldId, rank});
        BalancerEntry balancerEntry;
        if (rank.length() >= 100) {
            balancerEntry = (new BalancerEntry.Builder(fieldId)).rebalanceTimeNow().build();
            this.balancerEntryManager.save(balancerEntry);
            this.submitScheduledBalance();
        } else if (rank.length() >= 50) {
            balancerEntry = (new BalancerEntry.Builder(fieldId)).rebalanceTime(DateTime.now().plusHours(12)).build();
            this.balancerEntryManager.save(balancerEntry);
        }

    }

    public boolean rankingOperationsDisabled(Long fieldId) {
        BalancerEntry entry = this.balancerEntryManager.get(fieldId);
        return entry == null ? false : entry.rankingOperationsDisabled();
    }

    public ServiceOutcome<Collection<Long>> requestFullBalance() {
        return this.scheduleBalance(this.lexoRankDao.findFieldIdsInLexoRankTable());
    }

    public ServiceOutcome<Collection<Long>> requestBalance(Long rankFieldId) {
        return !this.lexoRankDao.findFieldIdsInLexoRankTable().contains(rankFieldId) ? ServiceOutcomeImpl.error("rankFieldId", ErrorCollection.Reason.SERVER_ERROR, "gh.lexorank.balancer.error.invalid.field.id", new Object[]{rankFieldId}) : this.scheduleBalance(Lists.newArrayList(new Long[]{rankFieldId}));
    }

    private ServiceOutcome<Collection<Long>> scheduleBalance(Collection<Long> rankFieldIds) {
        Iterator var2 = rankFieldIds.iterator();

        while (var2.hasNext()) {
            Long rankFieldId = (Long) var2.next();
            BalancerEntry balancerEntry = (new BalancerEntry.Builder(rankFieldId)).rebalanceTimeNow().build();
            this.balancerEntryManager.save(balancerEntry);
        }

        ServiceOutcome<Void> outcome = this.submitScheduledBalance();
        return outcome.isInvalid() ? ServiceOutcomeImpl.error(outcome) : ServiceOutcomeImpl.ok(rankFieldIds);
    }

    public ServiceOutcome<Void> submitScheduledBalance() {
        if (this.isBalancingDisabled()) {
            //平衡禁用
            this.log.debug("Balancing has been disabled (possibly due to foreground reindex) - rebalance not scheduled", new Object[0]);
            return ServiceOutcomeImpl.error(ErrorCollection.Reason.CONFLICT, "gh.lexorank.service.error.balancing.disabled", new Object[0]);
        } else if (this.lexoRankSuspendManager.isSuspended()) {
            //平衡被暂停
            this.log.debug("Balancing has been manually disabled by an admin, and must be enabled manually - rebalance not scheduled", new Object[0]);
            return ServiceOutcomeImpl.error(ErrorCollection.Reason.CONFLICT, "gh.lexorank.service.error.balancing.suspended", new Object[0]);
        } else if (this.shouldBalancingBackOff()) {
            //应该平衡但是节点索引阈值的延迟>缓存中的阈值延迟数据
            this.log.debug("Balancing has been backed off because there are some nodes that are lagging behind with index recovery", new Object[0]);
            return ServiceOutcomeImpl.error(ErrorCollection.Reason.CONFLICT, "gh.lexorank.service.error.balancing.backoff", new Object[0]);
        } else if (this.lexoRankScheduledBalanceHandler.isRunning()) {
            //有一个平衡正在后台运行
            this.log.debug("Balance not scheduled because balance handler is already running", new Object[0]);
            return ServiceOutcomeImpl.error(ErrorCollection.Reason.CONFLICT, "gh.lexorank.service.error.balancing.in.progress", new Object[0]);
        } else {
            //执行平衡任务
            this.log.debug("Submiting balance handler task to the executor service", new Object[0]);
            this.executorService.submit(this.lexoRankScheduledBalanceHandler);
            return ServiceOutcomeImpl.ok();
        }
    }

    @VisibleForTesting
    public ThreadPoolExecutor getExecutorService() {
        return this.executorService;
    }

    private boolean isForegroundReindexRunning() {
        TaskDescriptor<? extends Serializable> liveTask = this.taskManager.getLiveTask(new ForegroundIndexTaskContext());
        return liveTask != null && liveTask.isStarted() && !liveTask.isFinished() && !liveTask.isCancelled();
    }

    private void shutdown() {
        if (this.isServiceInitialised.get() && this.isServiceShutdown.compareAndSet(false, true)) {
            this.log.info("LexoRank Balancing Service shutting down", new Object[0]);
            this.isBalancingDisabled.set(true);
            this.compatibilityPluginScheduler.unregisterJobHandler(LexoRankBalancePluginJob.JOB_HANDLER_KEY);

            do {
                this.executorService.shutdown();

                try {
                    if (!this.executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
                        this.executorService.shutdownNow();
                        if (!this.executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
                            throw new RuntimeException("LexoRank executor did not terminate");
                        }
                    }
                } catch (InterruptedException var2) {
                    this.executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            } while (!this.executorService.isTerminated());

            this.log.info("LexoRank Balancing Service has shut down", new Object[0]);
        }

    }

    private void enableBalancing() {
        if (this.isServiceInitialised.get() && !this.isServiceShutdown.get() && this.isBalancingDisabled.compareAndSet(true, false)) {
            this.log.info("Balancing Enabled", new Object[0]);
        }

    }

    private void disableBalancing() {
        if (this.isServiceInitialised.get() && !this.isServiceShutdown.get() && this.isBalancingDisabled.compareAndSet(false, true)) {
            this.log.info("Balancing Disabled", new Object[0]);
        }

    }

    public LexoRankBalancingService.LexoRankBalancingServiceStatus getBalanceStatus() {
        return new LexoRankBalancingService.LexoRankBalancingServiceStatus(this.isBalancingDisabled(), this.lexoRankSuspendManager.isSuspended(), this.lexoRankScheduledBalanceHandler.isRunning());
    }

    public static class LexoRankBalancingServiceStatus {
        public final Boolean balancingDisabled;
        public final Boolean balancingSuspended;
        public final Boolean balanceHandlerRunning;

        public LexoRankBalancingServiceStatus(boolean balancingDisabled, boolean balancingSuspended, boolean balanceHandlerRunning) {
            this.balancingDisabled = Boolean.valueOf(balancingDisabled);
            this.balancingSuspended = Boolean.valueOf(balancingSuspended);
            this.balanceHandlerRunning = Boolean.valueOf(balanceHandlerRunning);
        }
    }

    private class LexoRankExecutor extends ThreadPoolExecutor {
        public LexoRankExecutor() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), (new ThreadFactoryBuilder()).setNameFormat("lexorank-executor-thread-%d").build());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, (Throwable) t);
            if (t == null && r instanceof Future) {
                try {
                    Future<?> future = (Future) r;
                    if (future.isDone()) {
                        future.get();
                    }
                } catch (CancellationException var4) {
                    t = var4;
                } catch (ExecutionException var5) {
                    t = var5.getCause();
                } catch (InterruptedException var6) {
                    Thread.currentThread().interrupt();
                }
            }

            if (t != null) {
                LexoRankBalancingService.this.log.exception((Throwable) t);
            }

        }
    }
}
