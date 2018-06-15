package com.example.demo.auditlog;

import org.joda.time.DateTime;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/15
 */
public class TimeTrackingChange {
    @XmlTransient
    public DateTime changeDateObject;
    @XmlElement
    public Long oldEstimate;
    @XmlElement
    public Long newEstimate;
    @XmlElement
    public Long timeSpent;
    @XmlElement
    public Long changeDate;

    public TimeTrackingChange() {
    }
}
