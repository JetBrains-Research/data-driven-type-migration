package org.jetbrains.research.ide.ui;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.util.treeView.AbstractTreeStructureBase;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;


/**
 * Copied from {@link com.intellij.refactoring.typeMigration.ui.TypeMigrationTreeStructure}
 */
public class FailedTypeChangesTreeStructure extends AbstractTreeStructureBase {
    private FailedTypeChangeRootNode myRoot;

    public FailedTypeChangesTreeStructure(final Project project) {
        super(project);
    }

    public void setRoots(final FailedTypeChangeRootNode root) {
        myRoot = root;
    }

    @Override
    public List<TreeStructureProvider> getProviders() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Object getRootElement() {
        return myRoot;
    }

    @Override
    public void commit() {

    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    @Override
    public boolean isToBuildChildrenInBackground(@NotNull final Object element) {
        return true;
    }
}