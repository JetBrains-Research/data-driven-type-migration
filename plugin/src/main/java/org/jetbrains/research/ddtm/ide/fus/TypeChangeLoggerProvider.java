package org.jetbrains.research.ddtm.ide.fus;

import com.intellij.internal.statistic.eventLog.StatisticsEventLoggerProvider;
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.registry.Registry;

import java.util.concurrent.TimeUnit;

public class TypeChangeLoggerProvider extends StatisticsEventLoggerProvider {
    public TypeChangeLoggerProvider() {
        super("DBP", 1, TimeUnit.HOURS.toMillis(1), "200KB");
    }

    @Override
    public boolean isRecordEnabled() {
        return !ApplicationManager.getApplication().isUnitTestMode() &&
                !ApplicationManager.getApplication().isHeadlessEnvironment() &&
                Registry.is("feature.usage.event.log.collect.and.upload") &&
                StatisticsUploadAssistant.isCollectAllowed();
    }

    @Override
    public boolean isSendEnabled() {
        return isRecordEnabled() && StatisticsUploadAssistant.isSendAllowed();
    }
}
