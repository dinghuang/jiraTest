package com.example.demo.test;

import com.atlassian.greenhopper.global.LoggerWrapper;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/5/23
 */
class LexoRankBalancerProgressLogger {
    private static final String PROGRESS_MESSAGE_TEMPLATE = "Balancing rank field with id[%d] - %,d out of %,d rows (%d%%) complete.";
    private static final String COMPLETION_MESSAGE_TEMPLATE = "Balanced rank field with id[%d] in %s.";
    private final long rankFieldId;
    private final long numRowsForField;
    private final long startTimestamp;
    private final LoggerWrapper loggerWrapper;
    private long numRowsBalanced;
    private long endTimestamp;
    private long lastLoggedPercentage;

    LexoRankBalancerProgressLogger(long rankFieldId, long numRowsForField, long numRowsBalanced) {
        this(rankFieldId, numRowsForField, numRowsBalanced, LoggerWrapper.with(LexoRankBalancerProgressLogger.class));
    }

    LexoRankBalancerProgressLogger(long rankFieldId, long numRowsForField, long numRowsBalanced, LoggerWrapper loggerWrapper) {
        this.rankFieldId = rankFieldId;
        this.numRowsForField = numRowsForField;
        this.numRowsBalanced = numRowsBalanced;
        this.startTimestamp = System.currentTimeMillis();
        this.loggerWrapper = loggerWrapper;
        this.lastLoggedPercentage = 0L;
        this.tryLog();
    }

    void makeProgress() {
        this.makeProgress(1L);
    }

    void makeProgress(long progressIncrement) {
        this.numRowsBalanced += progressIncrement;
        if (this.numRowsBalanced == this.numRowsForField) {
            this.endTimestamp = System.currentTimeMillis();
        }

        this.tryLog();
    }

    private void tryLog() {
        long progressPercentage = this.numRowsBalanced * 100L / this.numRowsForField;
        if (progressPercentage > this.lastLoggedPercentage) {
            this.loggerWrapper.info("Balancing rank field with id[%d] - %,d out of %,d rows (%d%%) complete.", new Object[]{Long.valueOf(this.rankFieldId), Long.valueOf(this.numRowsBalanced), Long.valueOf(this.numRowsForField), Long.valueOf(progressPercentage)});
            this.lastLoggedPercentage = progressPercentage;
        }

        if (this.numRowsBalanced == this.numRowsForField) {
            String balancingTimeStr = this.getFormattedBalancingTime();
            this.loggerWrapper.info("Balanced rank field with id[%d] in %s.", new Object[]{Long.valueOf(this.rankFieldId), balancingTimeStr});
        }

    }

    private String getFormattedBalancingTime() {
        PeriodFormatter periodFormatter = (new PeriodFormatterBuilder()).appendHours().appendSuffix(" hour", " hours").appendSeparator(" ").appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ").appendSeconds().appendSuffix(" second", " seconds").toFormatter();
        Duration duration = Duration.millis(this.endTimestamp - this.startTimestamp);
        return periodFormatter.print(duration.toPeriod());
    }
}