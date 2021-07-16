package org.jetbrains.research.ddtm.ide.refactoring.listeners;

import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.ide.fus.TypeChangeLogsCollector;

public class UndoTypeChangeListener implements CommandListener {
    @Override
    public void commandStarted(@NotNull CommandEvent event) {
        if (event.getProject() == null || event.getCommandName() == null) return;
        String expectedCommandNamePrefix = DataDrivenTypeMigrationBundle.message("group.id");
        if (event.getCommandName().startsWith("Undo " + expectedCommandNamePrefix)) {
            String[] typePair = event.getCommandName().substring(7 + expectedCommandNamePrefix.length()).split(" to ");
            TypeChangeLogsCollector.getInstance().migrationUndone(event.getProject(), typePair[0], typePair[1]);
        }
    }
}
