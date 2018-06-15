package com.example.demo.auditlog;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/15
 */
@XmlRootElement
public class BaseStatisticChange {
    @XmlElement
    public Double oldValue;
    @XmlElement
    public Double newValue;

    public BaseStatisticChange() {
    }

    public BaseStatisticChange(Double oldValue, Double newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
