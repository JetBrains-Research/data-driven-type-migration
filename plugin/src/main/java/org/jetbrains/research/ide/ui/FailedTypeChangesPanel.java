package org.jetbrains.research.ide.ui;

import com.intellij.core.JavaPsiBundle;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.TextChunk;
import com.intellij.usages.UsageInfoToUsageConverter;
import com.intellij.usages.UsagePresentation;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FailedTypeChangesPanel extends JPanel implements Disposable {
    private final List<PsiElement> myFailedUsages;
    private final MyTree myRootsTree;
    private Content myContent;

    public FailedTypeChangesPanel(List<PsiElement> failedUsages, Project project) {
        super(new BorderLayout());
        myFailedUsages = failedUsages;

        final FailedTypeChangeRootNode currentRoot = new FailedTypeChangeRootNode(project, failedUsages);

        myRootsTree = new MyTree();
        FailedTypeChangesTreeStructure structure = new FailedTypeChangesTreeStructure(project);
        structure.setRoots(currentRoot);
        final var model = new StructureTreeModel<>(structure, AlphaComparator.INSTANCE, this);

        myRootsTree.setModel(new AsyncTreeModel(model, this));
        initTree(myRootsTree);

        add(ScrollPaneFactory.createScrollPane(myRootsTree));
        add(createToolbar(), BorderLayout.SOUTH);

        model.invalidate();
    }

    private JComponent createToolbar() {
        // TODO: think about toolbar contents
        final JPanel panel = new JPanel(new GridBagLayout());
        final JButton performButton = new JButton("Test Button");
        panel.add(performButton);
        return panel;
    }

    public void setContent(final Content content) {
        myContent = content;
        Disposer.register(content, this);
    }

    private void initTree(final Tree tree) {
        tree.setCellRenderer(new FailedTypeChangesTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        TreeUtil.installActions(tree);
        TreeUtil.expandAll(tree);
        SmartExpander.installOn(tree);
        EditSourceOnDoubleClickHandler.install(tree);
        new TreeSpeedSearch(tree);
        // PopupHandler.installUnknownPopupHandler(tree, createTreePopupActions());
    }

    @Override
    public void dispose() {

    }

    private static final class MyTree extends Tree implements DataProvider {
        private static void collectInfos(final Set<? super UsageInfo> usageInfos,
                                         final FailedTypeChangeNode currentNode) {
            usageInfos.add(currentNode.getInfo());
            if (!currentNode.areChildrenInitialized()) return;
            for (var node : currentNode.getChildren()) {
                collectInfos(usageInfos, (FailedTypeChangeNode) node);
            }
        }

        @Override
        protected void paintComponent(final Graphics g) {
            DuplicateNodeRenderer.paintDuplicateNodesBackground(g, this);
            super.paintComponent(g);
        }

        @Override
        public Object getData(@NotNull @NonNls final String dataId) {
            if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
                final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes(DefaultMutableTreeNode.class, null);
                return selectedNodes.length == 1 && selectedNodes[0].getUserObject() instanceof FailedTypeChangeNode
                        ? ((FailedTypeChangeNode) selectedNodes[0].getUserObject()).getInfo().getElement() : null;
            }
            if ("migration.usages".equals(dataId)) {
                DefaultMutableTreeNode[] selectedNodes = getSelectedNodes(DefaultMutableTreeNode.class, null);
                final Set<UsageInfo> usageInfos = new HashSet<>();
                for (DefaultMutableTreeNode selectedNode : selectedNodes) {
                    final Object userObject = selectedNode.getUserObject();
                    if (userObject instanceof FailedTypeChangeNode) {
                        collectInfos(usageInfos, (FailedTypeChangeNode) userObject);
                    }
                }
                return usageInfos.toArray(UsageInfo.EMPTY_ARRAY);
            }
            return null;
        }
    }

    private static class FailedTypeChangesTreeCellRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull final JTree tree,
                                          final Object value,
                                          final boolean selected,
                                          final boolean expanded,
                                          final boolean leaf,
                                          final int row,
                                          final boolean hasFocus) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (!(userObject instanceof FailedTypeChangeNode)) return;
            final UsageInfo usageInfo = ((FailedTypeChangeNode) userObject).getInfo();
            if (usageInfo != null) {
                final PsiElement element = usageInfo.getElement();
                if (element != null) {
                    PsiElement typeElement = null;
                    if (element instanceof PsiVariable) {
                        typeElement = ((PsiVariable) element).getTypeElement();
                    } else if (element instanceof PsiMethod) {
                        typeElement = ((PsiMethod) element).getReturnTypeElement();
                    }
                    if (typeElement == null) typeElement = element;

                    final UsagePresentation presentation = UsageInfoToUsageConverter.convert(
                            new PsiElement[]{typeElement},
                            new UsageInfo(typeElement)
                    ).getPresentation();
                    boolean isPrefix = true;  //skip usage position
                    for (TextChunk chunk : presentation.getText()) {
                        append(chunk.getText(), chunk.getSimpleAttributesIgnoreBackground());
                        isPrefix = false;
                    }
                    setIcon(presentation.getIcon());

                    String location;
                    if (element instanceof PsiMember) {
                        location = SymbolPresentationUtil.getSymbolContainerText(element);
                    } else {
                        final PsiMember member = PsiTreeUtil.getParentOfType(element, PsiMember.class);
                        if (member instanceof PsiField) {
                            location = PsiFormatUtil.formatVariable((PsiVariable) member,
                                    PsiFormatUtilBase.SHOW_NAME |
                                            PsiFormatUtilBase.SHOW_CONTAINING_CLASS |
                                            PsiFormatUtilBase.SHOW_FQ_NAME, PsiSubstitutor.EMPTY);
                        } else if (member instanceof PsiMethod) {
                            location = PsiFormatUtil.formatMethod((PsiMethod) member, PsiSubstitutor.EMPTY,
                                    PsiFormatUtilBase.SHOW_NAME |
                                            PsiFormatUtilBase.SHOW_CONTAINING_CLASS |
                                            PsiFormatUtilBase.SHOW_FQ_NAME,
                                    PsiFormatUtilBase.SHOW_TYPE);
                        } else if (member instanceof PsiClass) {
                            location = PsiFormatUtil.formatClass((PsiClass) member, PsiFormatUtilBase.SHOW_NAME |
                                    PsiFormatUtilBase.SHOW_FQ_NAME);
                        } else {
                            location = null;
                        }
                        if (location != null) location = JavaPsiBundle.message("aux.context.display", location);
                    }
                    if (location != null) {
                        append(location, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                } else {
                    append(UsageViewBundle.message("node.invalid"), SimpleTextAttributes.ERROR_ATTRIBUTES);
                }
            }
        }
    }
}
