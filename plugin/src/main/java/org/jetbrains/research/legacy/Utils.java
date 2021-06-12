package org.jetbrains.research.legacy;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.data.TypeChangeRulesStorage;

public class Utils {
    private static final Logger LOG = Logger.getInstance(TypeChangeRulesStorage.class);

    public static void checkForCompilationErrors(PsiFile file, Document document, Project project) {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Performing code analysis...") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            final ProgressIndicator daemonIndicator = new DaemonProgressIndicator();
                            ((ProgressIndicatorEx) indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {
                                @Override
                                public void cancel() {
                                    super.cancel();
                                    daemonIndicator.cancel();
                                }
                            });

                            final DaemonCodeAnalyzerImpl codeAnalyzer =
                                    (DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(project);
                            final var infos = DumbService.getInstance(project).runReadActionInSmartMode(() ->
                                    codeAnalyzer.runMainPasses(file, document, daemonIndicator)
                            );
                            LOG.debug(infos.toString());

                        } catch (ProcessCanceledException e) {
                            LOG.info("Code analysis canceled", e);
                        } catch (Exception e) {
                            LOG.error(e);
                        }
                    }
                });
            }
        });

    }
}
