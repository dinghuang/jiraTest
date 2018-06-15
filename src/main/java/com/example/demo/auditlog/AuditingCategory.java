package com.example.demo.auditlog;

import com.atlassian.annotations.ExperimentalApi;

import javax.annotation.Nullable;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/11
 */
@ExperimentalApi
public enum AuditingCategory {
    AUDITING("auditing", "jira.auditing.category"),
    USER_MANAGEMENT("user management", "jira.auditing.category.usermanagement"),
    GROUP_MANAGEMENT("group management", "jira.auditing.category.groupmanagement"),
    PERMISSIONS("permissions", "jira.auditing.category.permissions"),
    WORKFLOWS("workflows", "jira.auditing.category.workflows"),
    NOTIFICATIONS("notifications", "jira.auditing.category.notifications"),
    FIELDS("fields", "jira.auditing.category.fields"),
    PROJECTS("projects", "jira.auditing.category.projects"),
    SYSTEM("system", "jira.auditing.category.system"),
    MIGRATION("migration", "jira.auditing.category.migration"),
    APPLICATIONS("applications", "jira.auditing.category.applications");

    private final String id;
    private final String nameI18nKey;

    private AuditingCategory(String id, String nameI18nKey) {
        this.id = id;
        this.nameI18nKey = nameI18nKey;
    }

    public String getId() {
        return this.id;
    }

    public String getNameI18nKey() {
        return this.nameI18nKey;
    }

    @Nullable
    public static AuditingCategory getCategoryById(String categoryId) {
        AuditingCategory[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            AuditingCategory category = var1[var3];
            if(category.getId().equals(categoryId)) {
                return category;
            }
        }

        return null;
    }

    @Nullable
    public static AuditingCategory getCategoryByIdOrName(String idOrName) {
        AuditingCategory[] var1 = values();
        int var2 = var1.length;

        int var3;
        AuditingCategory category;
        for(var3 = 0; var3 < var2; ++var3) {
            category = var1[var3];
            if(category.getId().equals(idOrName)) {
                return category;
            }
        }

        var1 = values();
        var2 = var1.length;

        for(var3 = 0; var3 < var2; ++var3) {
            category = var1[var3];
            if(category.name().equals(idOrName)) {
                return category;
            }
        }

        return null;
    }
}
