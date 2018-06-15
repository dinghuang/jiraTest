package com.example.demo.auditlog;

import com.atlassian.jira.ofbiz.FieldMap;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import org.ofbiz.core.entity.GenericValue;

/**
 * jira审计日志
 * 审核日志不记录人群中的所有活动。它会记录可能影响安全性或Crowd设置的配置更改。
 *
 * @author dinghuang123@gmail.com
 * @since 2018/6/11
 */
public class AuditLogDTO {
    private Long id;
    /**
     * 远程地址
     */
    private String remoteAddress;
    /**
     * 创建时间
     */
    private java.sql.Timestamp created;
    /**
     * 作者
     */
    private String authorKey;
    /**
     * 概要
     */
    private String summary;
    /**
     * 种类
     */
    private String category;
    /**
     * 对象类型
     */
    private String objectType;
    /**
     * 项目id
     */
    private String objectId;
    /**
     * 项目名称
     */
    private String objectName;
    /**
     * 对象的父id（默认10000）
     */
    private String objectParentId;
    /**
     * 对象父名称（默认LDAP server）
     */
    private String objectParentName;
    /**
     * 操作人类型
     */
    private Integer authorType;
    /**
     * 事件源名称
     */
    private String eventSourceName;
    /**
     * 检索字段（需要检索字段的toString）
     */
    private String searchField;

    public Long getId() {
        return id;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public java.sql.Timestamp getCreated() {
        return created;
    }

    public String getAuthorKey() {
        return authorKey;
    }

    public String getSummary() {
        return summary;
    }

    public String getCategory() {
        return category;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectParentId() {
        return objectParentId;
    }

    public String getObjectParentName() {
        return objectParentName;
    }

    public Integer getAuthorType() {
        return authorType;
    }

    public String getEventSourceName() {
        return eventSourceName;
    }

    public String getSearchField() {
        return searchField;
    }

    public AuditLogDTO(Long id, String remoteAddress, java.sql.Timestamp created, String authorKey, String summary, String category, String objectType, String objectId, String objectName, String objectParentId, String objectParentName, Integer authorType, String eventSourceName, String searchField) {
        this.id = id;
        this.remoteAddress = remoteAddress;
        this.created = created;
        this.authorKey = authorKey;
        this.summary = summary;
        this.category = category;
        this.objectType = objectType;
        this.objectId = objectId;
        this.objectName = objectName;
        this.objectParentId = objectParentId;
        this.objectParentName = objectParentName;
        this.authorType = authorType;
        this.eventSourceName = eventSourceName;
        this.searchField = searchField;
    }

    /**
     * Creates a GenericValue object from the values in this Data Transfer Object.
     * <p>
     * This can be useful when QueryDsl code needs to interact with legacy OfBiz code.
     *
     * @param ofBizDelegator OfBizDelegator will have makeValue() called on it.
     * @return a GenericValue object constructed from the values in this Data Transfer Object.
     */
    public GenericValue toGenericValue(final OfBizDelegator ofBizDelegator) {
        return ofBizDelegator.makeValue("AuditLog", new FieldMap()
                .add("id", id)
                .add("remoteAddress", remoteAddress)
                .add("created", created)
                .add("authorKey", authorKey)
                .add("summary", summary)
                .add("category", category)
                .add("objectType", objectType)
                .add("objectId", objectId)
                .add("objectName", objectName)
                .add("objectParentId", objectParentId)
                .add("objectParentName", objectParentName)
                .add("authorType", authorType)
                .add("eventSourceName", eventSourceName)
                .add("searchField", searchField)
        );
    }

    /**
     * Constructs a new instance of this Data Transfer object from the values in the given GenericValue.
     * <p>
     * This can be useful when QueryDsl code needs to interact with legacy OfBiz code.
     *
     * @param gv the GenericValue
     * @return a new instance of this Data Transfer object with the values in the given GenericValue.
     */

    public static com.atlassian.jira.model.querydsl.AuditLogDTO fromGenericValue(GenericValue gv) {
        return new com.atlassian.jira.model.querydsl.AuditLogDTO(
                gv.getLong("id"),
                gv.getString("remoteAddress"),
                gv.getTimestamp("created"),
                gv.getString("authorKey"),
                gv.getString("summary"),
                gv.getString("category"),
                gv.getString("objectType"),
                gv.getString("objectId"),
                gv.getString("objectName"),
                gv.getString("objectParentId"),
                gv.getString("objectParentName"),
                gv.getInteger("authorType"),
                gv.getString("eventSourceName"),
                gv.getString("searchField")
        );
    }
}
