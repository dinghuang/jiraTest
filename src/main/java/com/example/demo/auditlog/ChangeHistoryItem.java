package com.example.demo.auditlog;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.util.dbc.Assertions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.annotation.concurrent.Immutable;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 更改日志事件
 *
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
@Immutable
@PublicApi
public class ChangeHistoryItem implements Comparable<ChangeHistoryItem> {
    private final Long id;
    private final Long changeGroupId;
    private final String userKey;
    private final String field;
    private final Long projectId;
    private final Long issueId;
    private final String issueKey;
    private final Timestamp created;
    private final Timestamp nextChangeCreated;
    private final Map<String, String> fromValues;
    private final Map<String, String> toValues;
    private static final Timestamp TS_MAX = new Timestamp(9223372036854775807L);

    public ChangeHistoryItem(Long id, Long changeGroupId, Long projectId, Long issueId, String issueKey, String field, Timestamp created, String from, String to, String fromValue, String toValue, String userKey) {
        this(id, changeGroupId, projectId, issueId, issueKey, field, created, new Timestamp(9223372036854775807L), from, to, fromValue, toValue, userKey);
    }

    public ChangeHistoryItem(Long id, Long changeGroupId, Long projectId, Long issueId, String issueKey, String field, Timestamp created, Timestamp nextChange, String from, String to, String fromValue, String toValue, String userKey) {
        this.fromValues = Maps.newHashMap();
        this.toValues = Maps.newHashMap();
        this.field = field;
        this.id = id;
        this.changeGroupId = changeGroupId;
        this.userKey = userKey;
        this.projectId = projectId;
        this.issueId = issueId;
        this.issueKey = issueKey;
        this.created = created;
        this.nextChangeCreated = nextChange;
        if (fromValue != null) {
            this.fromValues.put(fromValue, from == null ? "" : from);
        }

        if (toValue != null) {
            this.toValues.put(toValue, to == null ? "" : to);
        }

    }

    private ChangeHistoryItem(Long id, Long changeGroupId, Long projectId, Long issueId, String issueKey, String field, Timestamp created, Timestamp nextChange, Map<String, String> fromValues, Map<String, String> toValues, String userKey) {
        this.fromValues = fromValues;
        this.toValues = toValues;
        this.id = id;
        this.changeGroupId = changeGroupId;
        this.userKey = userKey;
        this.projectId = projectId;
        this.issueId = issueId;
        this.issueKey = issueKey;
        this.created = created;
        this.nextChangeCreated = nextChange;
        this.field = field;
    }

    public Long getId() {
        return this.id;
    }

    public Long getChangeGroupId() {
        return this.changeGroupId;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getUser() {
        return this.userKey;
    }

    public String getUserKey() {
        return this.userKey;
    }

    public Long getProjectId() {
        return this.projectId;
    }

    public Long getIssueId() {
        return this.issueId;
    }

    public String getIssueKey() {
        return this.issueKey;
    }

    public Timestamp getCreated() {
        return this.created;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getFrom() {
        String from = null;
        if (this.fromValues.size() > 0) {
            from = (String) this.fromValues.values().iterator().next();
        }

        return from;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getTo() {
        String to = null;
        if (this.toValues.size() > 0) {
            to = (String) this.toValues.values().iterator().next();
        }

        return to;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getFromValue() {
        String fromValue = null;
        if (this.fromValues.size() > 0) {
            fromValue = (String) this.fromValues.keySet().iterator().next();
        }

        return fromValue;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getToValue() {
        String toValue = null;
        if (this.toValues.size() > 0) {
            toValue = (String) this.toValues.keySet().iterator().next();
        }

        return toValue;
    }

    public Map<String, String> getFroms() {
        return ImmutableMap.copyOf(this.fromValues);
    }

    public Map<String, String> getTos() {
        return ImmutableMap.copyOf(this.toValues);
    }

    public String getField() {
        return this.field;
    }

    public Timestamp getNextChangeCreated() {
        return this.nextChangeCreated;
    }

    public Long getDuration() {
        return this.nextChangeCreated.equals(TS_MAX) ? Long.valueOf(-1L) : Long.valueOf(this.nextChangeCreated.getTime() - this.created.getTime());
    }

    public boolean containsFromValue(String fromValue) {
        return this.fromValues.keySet().contains(fromValue);
    }

    public boolean containsToValue(String toValue) {
        return this.toValues.keySet().contains(toValue);
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof ChangeHistoryItem) {
            ChangeHistoryItem rhs = (ChangeHistoryItem) o;
            return (new EqualsBuilder()).append(this.getId(), rhs.getId()).append(this.getChangeGroupId(), rhs.getChangeGroupId()).append(this.getField(), rhs.getField()).append(this.getUserKey(), rhs.getUserKey()).append(this.getProjectId(), rhs.getProjectId()).append(this.getIssueId(), rhs.getIssueId()).append(this.getIssueKey(), rhs.getIssueKey()).append(this.getCreated(), rhs.getCreated()).append(this.getNextChangeCreated(), rhs.getNextChangeCreated()).append(this.getFroms(), rhs.getFroms()).append(this.getTos(), rhs.getTos()).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (new HashCodeBuilder()).append(this.getId()).append(this.getChangeGroupId()).append(this.getField()).append(this.getUserKey()).append(this.getProjectId()).append(this.getIssueId()).append(this.getIssueKey()).append(this.getCreated()).append(this.getNextChangeCreated()).append(this.getFroms()).append(this.getTos()).toHashCode();
    }

    @Override
    public int compareTo(ChangeHistoryItem other) {
        int result = this.created.compareTo(other.getCreated());
        if (result == 0) {
            result = this.changeGroupId.compareTo(other.getChangeGroupId());
            if (result == 0) {
                result = this.id.compareTo(other.getId());
            }
        }

        return result;
    }

    public static class Builder {
        private Long id;
        private Long changeGroupId;
        private Long projectId;
        private Long issueId;
        private String issueKey;
        private String field;
        private Timestamp created;
        private Map<String, String> fromValues = Maps.newHashMap();
        private Map<String, String> toValues = Maps.newHashMap();
        private String userKey;
        private Timestamp nextChangeCreated = new Timestamp(9223372036854775807L);

        public Builder() {
        }

        public ChangeHistoryItem.Builder fromChangeItem(ChangeHistoryItem changeItem) {
            this.fromChangeItemWithoutPreservingChanges(changeItem);
            this.fromValues = Maps.newHashMap(changeItem.getFroms());
            this.toValues = Maps.newHashMap(changeItem.getTos());
            return this;
        }

        public ChangeHistoryItem.Builder fromChangeItemWithoutPreservingChanges(ChangeHistoryItem changeItem) {
            this.id = changeItem.getId();
            this.projectId = changeItem.getProjectId();
            this.changeGroupId = changeItem.getChangeGroupId();
            this.issueId = changeItem.getIssueId();
            this.issueKey = changeItem.getIssueKey();
            this.field = changeItem.getField();
            this.created = changeItem.getCreated();
            this.userKey = changeItem.getUserKey();
            this.nextChangeCreated = changeItem.getNextChangeCreated();
            return this;
        }

        public ChangeHistoryItem.Builder fromChangeItemPreservingFromValues(ChangeHistoryItem changeItem) {
            this.fromChangeItemWithoutPreservingChanges(changeItem);
            this.fromValues = Maps.newHashMap(changeItem.getFroms());
            return this;
        }

        public ChangeHistoryItem.Builder fromChangeItemPreservingToValues(ChangeHistoryItem changeItem) {
            this.fromChangeItemWithoutPreservingChanges(changeItem);
            this.toValues = Maps.newHashMap(changeItem.getTos());
            return this;
        }

        public ChangeHistoryItem.Builder withId(Long id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public ChangeHistoryItem.Builder withId(long id) {
            return this.withId(Long.valueOf(id));
        }

        public ChangeHistoryItem.Builder inChangeGroup(Long id) {
            Assertions.notNull(id);
            this.changeGroupId = id;
            return this;
        }

        /**
         * @deprecated
         */
        public ChangeHistoryItem.Builder inChangeGroup(long id) {
            return this.inChangeGroup(Long.valueOf(id));
        }

        public ChangeHistoryItem.Builder inProject(Long projectId) {
            Assertions.notNull(projectId);
            this.projectId = projectId;
            return this;
        }

        /**
         * @deprecated
         */
        public ChangeHistoryItem.Builder inProject(long projectId) {
            return this.inProject(Long.valueOf(projectId));
        }

        public ChangeHistoryItem.Builder forIssue(Long issueId, String issueKey) {
            Assertions.notNull(issueId);
            this.issueId = issueId;
            this.issueKey = issueKey == null ? "" : issueKey;
            return this;
        }

        /**
         * @deprecated
         */
        public ChangeHistoryItem.Builder forIssue(long issueId, String issueKey) {
            return this.forIssue(Long.valueOf(issueId), issueKey);
        }

        public ChangeHistoryItem.Builder field(String field) {
            Assertions.notNull(field);
            this.field = field;
            return this;
        }

        public ChangeHistoryItem.Builder changedFrom(String from, String fromValue) {
            if (fromValue != null) {
                this.fromValues.put(fromValue, from == null ? "" : from);
            }

            return this;
        }

        public ChangeHistoryItem.Builder to(String to, String toValue) {
            if (toValue != null) {
                this.toValues.put(toValue, to == null ? "" : to);
            }

            return this;
        }

        public ChangeHistoryItem.Builder byUser(String userKey) {
            this.userKey = userKey;
            return this;
        }

        public ChangeHistoryItem.Builder on(Timestamp created) {
            Assertions.notNull(created);
            this.created = created;
            return this;
        }

        public ChangeHistoryItem.Builder nextChangeOn(Timestamp nextChangeCreated) {
            this.nextChangeCreated = nextChangeCreated;
            return this;
        }

        public ChangeHistoryItem.Builder withTos(Map<String, String> tos) {
            this.toValues = Maps.newHashMap(tos);
            return this;
        }

        public ChangeHistoryItem.Builder withFroms(Map<String, String> froms) {
            this.fromValues = Maps.newHashMap(froms);
            return this;
        }

        public ChangeHistoryItem build() {
            return new ChangeHistoryItem(this.id, this.changeGroupId, this.projectId, this.issueId, this.issueKey, this.field, this.created, this.nextChangeCreated, this.fromValues, this.toValues, this.userKey);
        }
    }
}
