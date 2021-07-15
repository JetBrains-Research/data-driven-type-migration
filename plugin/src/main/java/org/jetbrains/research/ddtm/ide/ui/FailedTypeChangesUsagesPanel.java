package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.packageDependencies.ui.UsagesPanel;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;

import javax.swing.*;
import java.awt.*;

/**
 * This class is mostly copied from {@link UsagesPanel}, but it fixes the problem with JTree expansion
 * in {@link FailedTypeChangesUsagesPanel#showUsages} method.
 */
public class FailedTypeChangesUsagesPanel extends JPanel implements Disposable, DataProvider {
    protected final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final Project myProject;
    ProgressIndicator myCurrentProgress;
    private JComponent myCurrentComponent;
    private UsageView myCurrentUsageView;

    public FailedTypeChangesUsagesPanel(Project project) {
        super(new BorderLayout());
        myProject = project;
        setToInitialPosition();
    }

    private static JComponent createLabel(@Nls String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public void setToInitialPosition() {
        cancelCurrentFindRequest();
        setToComponent(createLabel(getInitialPositionText()));
    }

    public @Nls String getInitialPositionText() {
        return DataDrivenTypeMigrationBundle.message("tool.window.initial.position.text");
    }

    public @Nls String getCodeUsagesString() {
        return DataDrivenTypeMigrationBundle.message("tool.window.code.usages.string");
    }

    void cancelCurrentFindRequest() {
        if (myCurrentProgress != null) {
            myCurrentProgress.cancel();
        }
    }

    protected void showUsages(PsiElement @NotNull [] primaryElements, UsageInfo @NotNull [] usageInfos) {
        if (myCurrentUsageView != null) {
            Disposer.dispose(myCurrentUsageView);
        }
        try {
            Usage[] usages = UsageInfoToUsageConverter.convert(primaryElements, usageInfos);
            UsageViewPresentation presentation = new UsageViewPresentation();
            presentation.setCodeUsagesString(getCodeUsagesString());
            myCurrentUsageView = UsageViewManager.getInstance(myProject).createUsageView(UsageTarget.EMPTY_ARRAY, usages, presentation, null);
            setToComponent(myCurrentUsageView.getComponent());
            if (!((UsageViewImpl) myCurrentUsageView).isDisposed()) {
                ((UsageViewImpl) myCurrentUsageView).expandAll();
            }
        } catch (ProcessCanceledException e) {
            setToCanceled();
        }
    }

    private void setToCanceled() {
        setToComponent(createLabel(CodeInsightBundle.message("usage.view.canceled")));
    }

    final void setToComponent(@NotNull JComponent component) {
        AppUIExecutor.onWriteThread(ModalityState.any()).expireWith(myProject).execute(() -> {
            if (myCurrentComponent != null) {
                if (myCurrentUsageView != null && myCurrentComponent == myCurrentUsageView.getComponent()) {
                    Disposer.dispose(myCurrentUsageView);
                    myCurrentUsageView = null;
                }
                remove(myCurrentComponent);
            }
            myCurrentComponent = component;
            add(component, BorderLayout.CENTER);
            revalidate();
        });
    }

    @Override
    public void dispose() {
        if (myCurrentUsageView != null) {
            Disposer.dispose(myCurrentUsageView);
            myCurrentUsageView = null;
        }
    }

    @Override
    @Nullable
    @NonNls
    public Object getData(@NotNull @NonNls String dataId) {
        if (PlatformDataKeys.HELP_ID.is(dataId)) {
            return "ideaInterface.find";
        }
        return null;
    }
}
