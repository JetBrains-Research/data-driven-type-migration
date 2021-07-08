package org.jetbrains.research.ddtm.ide.refactoring;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.TypeChangeRulesStorage;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.migration.TypeChangeProcessor;
import org.jetbrains.research.ddtm.ide.refactoring.services.TypeChangeRefactoringProviderImpl;
import org.jetbrains.research.ddtm.ide.ui.TypeChangeGutterPopupPanel;

import javax.swing.*;
import java.awt.*;

public class TypeChangeGutterIconRenderer extends GutterIconRenderer {
    private final int offset;

    public TypeChangeGutterIconRenderer(int offset) {
        this.offset = offset;
    }

    @Override
    public @NotNull Icon getIcon() {
        return AllIcons.Gutter.SuggestedRefactoringBulb;
    }

    @Override
    public @Nullable String getTooltipText() {
        return DataDrivenTypeMigrationBundle.message("suggested.gutter.icon.tooltip.text");
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @Override
    public @NotNull Alignment getAlignment() {
        return Alignment.RIGHT;
    }

    @Override
    public @Nullable AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final var project = e.getDataContext().getData(CommonDataKeys.PROJECT);
                final var editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
                showRefactoringOpportunity(project, editor);
            }
        };
    }

    private void showRefactoringOpportunity(Project project, Editor editor) {
        final var state = TypeChangeRefactoringProviderImpl.getInstance(project).getState();
        final var optionalTypeChangeMarker = state.getCompletedTypeChangeForOffset(offset);
        if (optionalTypeChangeMarker.isEmpty()) return;
        final var typeChangeMarker = optionalTypeChangeMarker.get();

        final PsiFile psiFile = PsiDocumentManager.getInstance(project).getCachedPsiFile(editor.getDocument());
        if (psiFile == null) return;
        final PsiElement newElement = psiFile.findElementAt(offset);

        final var pattern = TypeChangeRulesStorage.findPattern(typeChangeMarker.sourceType, typeChangeMarker.targetType);
        if (pattern.isEmpty()) return;

        final var data = SuggestedRefactoringData.getInstance();
        data.project = project;
        data.context = newElement;
        data.pattern = pattern.get();

        final var popup = new TypeChangeGutterPopupPanel(pattern.get().getSourceType(), pattern.get().getTargetType());
        final BalloonCallback callback = createAndShowBalloon(
                popup,
                editor,
                this::doRefactoring
        );
        popup.onRefactor = callback.onApply;
    }

    private void doRefactoring() {
        final var data = SuggestedRefactoringData.getInstance();
        final var processor = new TypeChangeProcessor(data.project, true);
        WriteCommandAction.runWriteCommandAction(data.project, () -> processor.run(data.context, data.pattern));
    }

    private BalloonCallback createAndShowBalloon(JComponent content, Editor editor, Runnable doRefactoring) {
        final var builder = JBPopupFactory.getInstance()
                .createDialogBalloonBuilder(content, null)
                .setRequestFocus(true)
                .setHideOnClickOutside(true)
                .setCloseButtonEnabled(false)
                .setAnimationCycle(0)
                .setBlockClicksThroughBalloon(true)
                .setContentInsets(JBUI.emptyInsets());
        final var borderColor = UIManager.getColor("InplaceRefactoringPopup.borderColor");
        if (borderColor != null) {
            builder.setBorderColor(borderColor);
        }

        final var balloon = builder.createBalloon();

        Runnable hideBalloonAndRefactor = () -> {
            balloon.hide(true);
            doRefactoring.run();
        };

        final var gutterComponent = ((EditorEx) editor).getGutterComponentEx();
        final var anchor = gutterComponent.getCenterPoint(this);
        if (anchor != null) {
            balloon.show(new RelativePoint(gutterComponent, anchor), Balloon.Position.below);
        } else {
            final var caretXY = editor.offsetToXY(editor.getCaretModel().getOffset());
            final var top = new RelativePoint(editor.getContentComponent(), caretXY);
            balloon.show(
                    new RelativePoint(editor.getContentComponent(), new Point(caretXY.x, top.getOriginalPoint().y)),
                    Balloon.Position.above
            );
        }

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                hideBalloonAndRefactor.run();
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), content, balloon);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                balloon.hide(false);
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ESCAPE"), content, balloon);

        LaterInvocator.enterModal(balloon);
        Disposer.register(balloon, () -> LaterInvocator.leaveModal(balloon));

        return new BalloonCallback(hideBalloonAndRefactor);
    }


    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

class SuggestedRefactoringData {
    private static volatile SuggestedRefactoringData INSTANCE;

    public Project project;
    public PsiElement context;
    public TypeChangePatternDescriptor pattern;

    private SuggestedRefactoringData() {
    }

    public static SuggestedRefactoringData getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final SuggestedRefactoringData INSTANCE = new SuggestedRefactoringData();
    }
}

class BalloonCallback {
    public Runnable onApply;

    public BalloonCallback(Runnable onApply) {
        this.onApply = onApply;
    }
}