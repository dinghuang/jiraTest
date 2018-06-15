package com.example.demo.auditlog;

import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * 燃尽图返回信息
 *
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
@XmlRootElement
public class BurndownChange {
    @XmlTransient
    public DateTime date;
    @XmlElement
    public String key;
    @XmlElement
    public BaseStatisticChange statC;
    @XmlElement
    //预估时间跟着时间的变化
    public TimeTrackingChange timeC;
    @XmlElement
    public Boolean added;
    @XmlElement
    public ColumnChange column;

    public BurndownChange() {
    }

    private BurndownChange(DateTime date, String key, BaseStatisticChange statC, TimeTrackingChange timeC, Boolean added, ColumnChange column) {
        this.date = date;
        this.key = key;
        this.statC = statC;
        this.timeC = timeC;
        this.added = added;
        this.column = column;
    }

    public void mergeChanges(BurndownChange other) {
        if (other.statC != null) {
            this.statC = other.statC;
        }

        if (other.added != null) {
            this.added = other.added;
        }

        if (other.column != null) {
            this.column = other.column;
        }

        if (other.timeC != null) {
            this.timeC = other.timeC;
        }

    }

    public boolean canMergeChange(BurndownChange other) {
        return this.date.compareTo(other.date) != 0 ? false : this.timeC == null || other.timeC == null;
    }

    public DateTime getDate() {
        return this.date;
    }

    public static BurndownChange.BurndownChangeBuilder builder() {
        return new BurndownChange.BurndownChangeBuilder();
    }

    public static class BurndownChangeBuilder {
        private DateTime date;
        private String key;
        private BaseStatisticChange statC;
        private TimeTrackingChange timeC;
        private Boolean added;
        private ColumnChange column;

        public BurndownChangeBuilder() {
        }

        public BurndownChange.BurndownChangeBuilder date(DateTime date) {
            this.date = date;
            return this;
        }

        public BurndownChange.BurndownChangeBuilder key(String key) {
            this.key = key;
            return this;
        }

        public BurndownChange.BurndownChangeBuilder statC(BaseStatisticChange statC) {
            this.statC = statC;
            return this;
        }

        public BurndownChange.BurndownChangeBuilder timeC(TimeTrackingChange timeC) {
            this.timeC = timeC;
            return this;
        }

        public BurndownChange.BurndownChangeBuilder added(Boolean added) {
            this.added = added;
            return this;
        }

        public BurndownChange.BurndownChangeBuilder column(ColumnChange column) {
            this.column = column;
            return this;
        }

        public BurndownChange build() {
            return new BurndownChange(this.date, this.key, this.statC, this.timeC, this.added, this.column);
        }
    }
}