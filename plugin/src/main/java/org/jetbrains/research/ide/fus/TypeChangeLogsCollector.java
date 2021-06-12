package org.jetbrains.research.ide.fus;

import com.intellij.concurrency.JobScheduler;
import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.research.data.models.TypeChangeRuleDescriptor;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class TypeChangeLogsCollector {
    private static final Integer LOG_DELAY_MIN = 24 * 60;
    private static final Integer LOG_INITIAL_DELAY_MIN = 11;
    // TODO: approve group id
    private static final EventLogGroup group = new EventLogGroup("dbp.ddtc.count", TypeChangeLogger.version);
    private static TypeChangeLogsCollector instance;

    private TypeChangeLogsCollector() {
        JobScheduler.getScheduler().scheduleWithFixedDelay(
                TypeChangeLogsCollector::trackRegistered,
                LOG_INITIAL_DELAY_MIN.longValue(),
                LOG_DELAY_MIN.longValue(),
                TimeUnit.MINUTES
        );
    }

    public static TypeChangeLogsCollector getInstance() {
        if (instance == null) {
            instance = new TypeChangeLogsCollector();
        }
        return instance;
    }

    private static void trackRegistered() {
        TypeChangeLogger.log(group, "registered");
    }

    public void proactiveIntentionApplied(Project project, String sourceType, String targetType, PsiElement root,
                                          int usagesUpdated, int suspiciousUsagesFound, int usagesFailed) {
        FeatureUsageData data = buildIntentionUsageData(
                project, sourceType, targetType, root, usagesUpdated, suspiciousUsagesFound, usagesFailed
        ).addData("invocation_workflow", "proactive");
        TypeChangeLogger.log(group, "proactive.intention.applied", data);
    }

    public void reactiveIntentionApplied(Project project, String sourceType, String targetType, PsiElement root,
                                         int usagesUpdated, int suspiciousUsagesFound, int usagesFailed) {
        FeatureUsageData data = buildIntentionUsageData(
                project, sourceType, targetType, root, usagesUpdated, suspiciousUsagesFound, usagesFailed
        ).addData("invocation_workflow", "reactive");
        TypeChangeLogger.log(group, "proactive.intention.applied", data);
    }

    public void recoveringIntentionApplied(Project project, TypeChangeRuleDescriptor rule) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("expression_before", rule.getExpressionBefore())
                .addData("expression_after", rule.getExpressionAfter())
                .addData("invocation_workflow", "recovering");
        TypeChangeLogger.log(group, "proactive.intention.applied", data);
    }

    private FeatureUsageData buildIntentionUsageData(Project project, String sourceType, String targetType, PsiElement root,
                                                     int usagesUpdated, int suspiciousUsagesFound, int usagesFailed) {
        return new FeatureUsageData().addProject(project)
                .addData("source_type", sourceType)
                .addData("target_type", targetType)
                .addData("migration_root", root.getClass().getName())
                .addData("usages_updated", usagesUpdated)
                .addData("suspicious_usages_found", suspiciousUsagesFound)
                .addData("usages_failed", usagesFailed);
    }
}
