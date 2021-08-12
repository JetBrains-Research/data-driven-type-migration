package org.jetbrains.research.ddtm.ide.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiType;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.ddtm.DataDrivenTypeMigrationBundle;
import org.jetbrains.research.ddtm.data.enums.SupportedSearchScope;
import org.jetbrains.research.ddtm.data.models.TypeChangePatternDescriptor;
import org.jetbrains.research.ddtm.ide.settings.TypeChangeSettingsState;
import org.jetbrains.research.ddtm.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class TypeChangeClassicModePanel extends JPanel {
    private final ComboBox<String> searchScopeOptionsComboBox =
            new ComboBox<>(DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.values().toArray(String[]::new));
    private final ComboBox<String> typeMigrationRulesComboBox;
    private final Map<String, TypeChangePatternDescriptor> textToPattern;

    public TypeChangeClassicModePanel(List<TypeChangePatternDescriptor> rulesDescriptors, PsiType sourceType, Project project) {
        super.setLayout(new BorderLayout());

        textToPattern = rulesDescriptors.stream().collect(Collectors.toMap(
                pattern -> StringUtils.escapeSSRTemplates(pattern.resolveTargetType(sourceType, project)),
                Function.identity()
        ));
        typeMigrationRulesComboBox = new ComboBox<>(textToPattern.keySet().toArray(String[]::new));
        typeMigrationRulesComboBox.setMinimumAndPreferredWidth(500);

        final JPanel panel = FormBuilder.createFormBuilder()
                .setHorizontalGap(10)
                .addLabeledComponent(
                        new JBLabel(DataDrivenTypeMigrationBundle.message("dialog.migration.rules.combobox.label",
                                escapeHtml(StringUtils.escapeSSRTemplates(sourceType.getCanonicalText())))),
                        typeMigrationRulesComboBox,
                        5,
                        true
                )
                .addComponentFillVertically(new JPanel(), 0)
                .addLabeledComponent(
                        new JBLabel(DataDrivenTypeMigrationBundle.message("settings.scope.combobox.label")),
                        searchScopeOptionsComboBox,
                        5,
                        true
                )
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        add(panel);
        this.setSearchScopeOption(TypeChangeSettingsState.getInstance().searchScope);
        this.setBorder(JBUI.Borders.empty(5, 2));
    }

    public TypeChangePatternDescriptor getSelectedPatternDescriptor() {
        return textToPattern.get(typeMigrationRulesComboBox.getSelectedItem());
    }

    public SupportedSearchScope getSearchScopeOption() {
        return DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.inverse().get(searchScopeOptionsComboBox.getSelectedItem());
    }

    public void setSearchScopeOption(@NotNull SupportedSearchScope searchScope) {
        searchScopeOptionsComboBox.setSelectedItem(DataDrivenTypeMigrationBundle.SEARCH_SCOPE_OPTIONS.get(searchScope));
    }
}
