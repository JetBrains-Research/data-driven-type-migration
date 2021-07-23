package org.jetbrains.research.ddtm.ide.fus;

import com.intellij.concurrency.JobScheduler;
import com.intellij.internal.statistic.eventLog.EventLogGroup;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.research.ddtm.data.enums.InvocationWorkflow;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class TypeChangeLogsCollector {
    private static final Integer LOG_DELAY_MIN = 24 * 60;
    private static final Integer LOG_INITIAL_DELAY_MIN = 5;
    // TODO: approve group id
    private static final EventLogGroup group = new EventLogGroup("dbp.ddtm.count", TypeChangeLogger.version);
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

    public void migrationUndone(Project project, int typeChangeId) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("type_change_id", typeChangeId);
        TypeChangeLogger.log(group, "migration.undone", data);
    }

    public void renamePerformed(Project project, String elementCanonicalName) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("element_canonical_name", elementCanonicalName);
        TypeChangeLogger.log(group, "rename.performed", data);
    }

    public void refactoringIntentionApplied(Project project, int typeChangeId, PsiElement root, int uniqueRulesUsed,
                                            int usagesUpdated, int suspiciousUsagesFound, int usagesFailed,
                                            InvocationWorkflow workflow) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("type_change_id", typeChangeId)
                .addData("migration_root", root.getClass().getName())
                .addData("unique_rules_used", uniqueRulesUsed)
                .addData("usages_updated", usagesUpdated)
                .addData("suspicious_usages_found", suspiciousUsagesFound)
                .addData("usages_failed", usagesFailed - suspiciousUsagesFound) // because every suspicious is also failed
                .addData("invocation_workflow", workflow.name().toLowerCase());
        TypeChangeLogger.log(group, "refactoring.intention.applied", data);
    }

    public void gutterIconClicked() {
        TypeChangeLogger.log(group, "gutter.icon.clicked");
    }

    public void recoveringIntentionApplied(Project project, int typeChangeId) {
        FeatureUsageData data = new FeatureUsageData().addProject(project)
                .addData("type_change_id", typeChangeId);
        TypeChangeLogger.log(group, "recovering.intention.applied", data);
    }
}
