package org.jetbrains.research.ide.suggested;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TypeMigrationGutterIconRenderer extends GutterIconRenderer {
    private final int offset;

    public TypeMigrationGutterIconRenderer(int offset) {
        this.offset = offset;
    }

    @Override
    public @NotNull Icon getIcon() {
        return AllIcons.Gutter.SuggestedRefactoringBulb;
    }

    @Override
    public @Nullable String getTooltipText() {
        return "Suggested type migration refactoring";
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
                final var popup = new TypeMigrationGutterPopup(() -> {
                });
                createAndShowBalloon(popup, editor);
            }
        };
    }

    private void createAndShowBalloon(JComponent content, Editor editor) {
        final var builder = JBPopupFactory.getInstance()
                .createDialogBalloonBuilder(content, null)
                .setRequestFocus(true)
                .setHideOnClickOutside(true)
                .setCloseButtonEnabled(false)
                .setAnimationCycle(0)
                .setBlockClicksThroughBalloon(true)
                .setContentInsets(JBUI.emptyInsets());

        final var balloon = builder.createBalloon();

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
                // MIGRATE
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
