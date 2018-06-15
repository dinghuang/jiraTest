package com.example.demo.sprintreport;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/6/12
 */
public abstract class RestTemplate {
    public RestTemplate() {
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE, false, com.atlassian.greenhopper.web.rapid.RestTemplate.class);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(3, 41, this, false, com.atlassian.greenhopper.web.rapid.RestTemplate.class);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, false, com.atlassian.greenhopper.web.rapid.RestTemplate.class);
    }
}
