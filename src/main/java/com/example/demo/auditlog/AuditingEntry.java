package com.example.demo.auditlog;

import com.atlassian.jira.auditing.*;
import com.atlassian.jira.user.ApplicationUser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/11
 */
public class AuditingEntry {
    public static AuditingEntry.Builder builder(@Nonnull AuditingCategory category,
                                                @Nonnull String summaryI18nKey,
                                                @Nonnull String eventSource) {
        return new AuditingEntry.Builder(category, summaryI18nKey, eventSource);
    }

    public static class Builder {
        private final AuditingCategory category;
        private final String summaryI18nKey;
        private final String eventSource;

        private boolean isAuthorSysAdmin;
        private String categoryName;
        private ApplicationUser author;
        private String remoteAddress;
        private AssociatedItem associatedObject;
        private Iterable<ChangedValue> changedValues;
        private Iterable<AssociatedItem> associatedItems;
        private String description;

        public Builder(@Nonnull AuditingCategory category,
                       @Nonnull String summaryI18nKey,
                       @Nonnull String eventSource) {
            this.category = notNull("category", category);
            this.summaryI18nKey = notNull("summaryI18nKey", summaryI18nKey);
            this.eventSource = notNull("eventSource", eventSource);
        }

        public AuditingEntry.Builder categoryName(String name) {
            this.categoryName = name;
            return this;
        }

        public AuditingEntry.Builder author(ApplicationUser author) {
            this.author = author;
            return this;
        }

        public AuditingEntry.Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        public AuditingEntry.Builder objectItem(final AssociatedItem object) {
            this.associatedObject = object;
            return this;
        }

        public AuditingEntry.Builder changedValues(final Iterable<ChangedValue> changedValues) {
            this.changedValues = changedValues;
            return this;
        }

        public AuditingEntry.Builder associatedItems(final Iterable<AssociatedItem> items) {
            this.associatedItems = items;
            return this;
        }

        public AuditingEntry.Builder isAuthorSysAdmin(final boolean isAuthorSysAdmin) {
            this.isAuthorSysAdmin = isAuthorSysAdmin;
            return this;
        }

        public AuditingEntry.Builder description(final String description) {
            this.description = description;
            return this;
        }

        public AuditingEntry build() {
            return new AuditingEntry(this);
        }
    }

    @Nonnull
    private final AuditingCategory category;
    @Nonnull
    private final String summaryI18nKey;
    @Nonnull
    private final String eventSource;

    private final boolean isAuthorSysAdmin;
    @Nullable
    private final String categoryName;
    @Nullable
    private final ApplicationUser author;
    @Nullable
    private final String remoteAddress;
    @Nullable
    private final AssociatedItem associatedItem;
    @Nullable
    private final Iterable<ChangedValue> changedValues;
    @Nullable
    private final Iterable<AssociatedItem> associatedItems;
    @Nullable
    private final String description;

    private AuditingEntry(AuditingEntry.Builder builder) {
        this.category = builder.category;
        this.categoryName = builder.categoryName;
        this.summaryI18nKey = builder.summaryI18nKey;
        this.eventSource = builder.eventSource;
        this.author = builder.author;
        this.remoteAddress = builder.remoteAddress;
        this.associatedItem = builder.associatedObject;
        this.changedValues = builder.changedValues;
        this.associatedItems = builder.associatedItems;
        this.isAuthorSysAdmin = builder.isAuthorSysAdmin;
        this.description = builder.description;
    }

    @Nonnull
    public AuditingCategory category() {
        return category;
    }

    @Nonnull
    public String summaryI18nKey() {
        return summaryI18nKey;
    }

    @Nonnull
    public String eventSource() {
        return eventSource;
    }

    public boolean authorIsSysAdmin() {
        return isAuthorSysAdmin;
    }

    @Nullable
    public String categoryName() {
        return categoryName;
    }

    @Nullable
    public ApplicationUser author() {
        return author;
    }

    @Nullable
    public String remoteAddress() {
        return remoteAddress;
    }

    @Nullable
    public AssociatedItem objectItem() {
        return associatedItem;
    }

    @Nullable
    public Iterable<ChangedValue> changedValues() {
        return changedValues;
    }

    @Nullable
    public Iterable<AssociatedItem> associatedItems() {
        return associatedItems;
    }

    @Nullable
    public String description() {
        return description;
    }
}
