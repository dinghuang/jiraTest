package com.example.demo.auditlog;

import javax.xml.bind.annotation.XmlElement;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/15
 */
public class ColumnChange {
    @XmlElement
    public Boolean notDone;
    @XmlElement
    public Boolean done;
    @XmlElement
    public String newStatus;

    public ColumnChange() {
    }

    public ColumnChange(Boolean notDone, Boolean done, String newStatus) {
        this.notDone = notDone;
        this.done = done;
        this.newStatus = newStatus;
    }
}
