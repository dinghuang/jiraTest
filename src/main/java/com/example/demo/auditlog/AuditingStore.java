package com.example.demo.auditlog;

import com.atlassian.jira.auditing.*;
import com.atlassian.jira.user.ApplicationUser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 用于在db中保存审计条目的审计存储
 *
 * @author dinghuang123@gmail.com
 * @since 2018/6/11
 */
public interface AuditingStore {
    @Deprecated
    void storeRecord(@Nonnull AuditingCategory category, String categoryName, @Nonnull String summaryI18nKey, @Nonnull String eventSource,
                     @Nullable ApplicationUser author, @Nullable String remoteAddress, @Nullable AssociatedItem object,
                     @Nullable Iterable<ChangedValue> changedValues, @Nullable Iterable<AssociatedItem> associatedItems,
                     boolean isAuthorSysAdmin);

    /**
     * Stores a record based on the information in the supplied entry.
     *
     * @param entry contains the information to log
     * @since 7.0
     */
    void storeRecord(@Nonnull AuditingEntry entry);

    @Nonnull
    Records getRecords(@Nullable Long maxId, @Nullable Long sinceId, @Nullable Integer maxResults, Integer offset,
                       @Nullable AuditingFilter filter, boolean includeSysAdminActions);

    long countRecords(@Nullable Long maxId, @Nullable Long sinceId, boolean includeSysAdminActions);

    long removeRecordsOlderThan(long timestamp);

    long countRecordsOlderThan(long timestamp);
}