package org.jetbrains.research.ide.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class FailedTypeChangeRootNode extends AbstractTreeNode<List<PsiElement>> {
    private final List<PsiElement> myRoots;
    private List<FailedTypeChangeNode> myCachedChildren;


    protected FailedTypeChangeRootNode(Project project, final List<PsiElement> roots) {
        super(project, roots);
        myRoots = roots;
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        if (myCachedChildren == null) {
            myCachedChildren = new ArrayList<>();
            for (PsiElement root : myRoots) {
                final var info = new UsageInfo(root);
                final HashSet<UsageInfo> parents = new HashSet<>();
                parents.add(info);
                myCachedChildren.add(new FailedTypeChangeNode(myProject, info, parents));
            }
        }
        return myCachedChildren;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {

    }
}