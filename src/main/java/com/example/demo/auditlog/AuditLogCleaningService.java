package com.example.demo.auditlog;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.auditing.AuditingRetentionPeriod;
import com.atlassian.jira.auditing.AuditingStore;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.service.AbstractService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定期清理审核lig的服务
 *
 * @author dinghuang123@gmail.com
 * @since 2018/6/11
 */
public class AuditLogCleaningService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(com.atlassian.jira.service.services.auditing.AuditLogCleaningService.class);
    private final AuditingStore auditingStore;
    private final ApplicationProperties applicationProperties;

    public AuditLogCleaningService(AuditingStore auditingStore, ApplicationProperties applicationProperties) {
        this.auditingStore = auditingStore;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void run() {
        final AuditingRetentionPeriod retentionPeriod = configuredRetentionPeriod();
        if (retentionPeriod != null && !retentionPeriod.isUnlimited()) {
            final DateTime monthsIntoPast = retentionPeriod.monthsIntoPast();
            if (logger.isDebugEnabled()) {
                logger.debug("Removing logger entries older than " + monthsIntoPast);
            }
            final long entriesRemoved = auditingStore.removeRecordsOlderThan(monthsIntoPast.getMillis());
            if (logger.isDebugEnabled()) {
                logger.debug("Removed " + entriesRemoved + " entries");
            }
        } else {
            logger.debug("Log entries are kept indefinitely");
        }
    }

    private AuditingRetentionPeriod configuredRetentionPeriod() {
        return AuditingRetentionPeriod.getByValue(applicationProperties.getDefaultBackedString(APKeys.JIRA_OPTION_AUDITING_LOG_RETENTION_PERIOD_IN_MONTHS));
    }

    @Override
    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException {
        return getObjectConfiguration("AUDITLOGCLEANINGSERVICE", "services/com/atlassian/jira/service/services/auditing/auditlogcleaningservice.xml", null);
    }
}