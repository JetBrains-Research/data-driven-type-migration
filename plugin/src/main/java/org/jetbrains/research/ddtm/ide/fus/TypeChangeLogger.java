package org.jetbrains.research.ddtm.ide.fus;

import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.StatisticsEventLoggerKt;
import com.intellij.internal.statistic.eventLog.StatisticsEventLoggerProvider;

public class TypeChangeLogger {
    static final StatisticsEventLoggerProvider loggerProvider =
            StatisticsEventLoggerKt.getEventLogProvider("DBP");

    static public final Integer version = loggerProvider.getVersion();

    static public void log(EventLogGroup group, String action) {
        loggerProvider.getLogger().logAsync(group, action, false);
    }

    static public void log(EventLogGroup group, String action, FeatureUsageData data) {
        loggerProvider.getLogger().logAsync(group, action, data.build(), false);
    }
}
