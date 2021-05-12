package org.jetbrains.research.ide.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class FailedTypeChangeNode extends AbstractTreeNode<UsageInfo> {
    private final UsageInfo myInfo;
    private List<FailedTypeChangeNode> myCachedChildren;

    public FailedTypeChangeNode(Project project, UsageInfo info, final HashSet<? extends UsageInfo> parents) {
        super(project, info);
        myInfo = info;
    }

    public UsageInfo getInfo() {
        return myInfo;
    }

    public boolean areChildrenInitialized() {
        return myCachedChildren != null;
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        if (myCachedChildren == null) {
            myCachedChildren = new ArrayList<>();
        }
        return myCachedChildren;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {

    }
}
