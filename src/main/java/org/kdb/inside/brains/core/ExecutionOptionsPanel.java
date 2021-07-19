package org.kdb.inside.brains.core;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.kdb.inside.brains.lang.binding.EditorsBindingStrategy;

import javax.swing.*;
import java.awt.*;

public class ExecutionOptionsPanel extends JPanel {
    private JBCheckBox logQueries;
    private JBCheckBox normalizeQuery;
    private JBCheckBox showConnectionState;
    private final JBIntSpinner connectionStateTimeout = new JBIntSpinner(1000, 100, 10000, 500);

    private final ComboBox<EditorsBindingStrategy> strategies = new ComboBox<>(EditorsBindingStrategy.values());

    private final JBIntSpinner warningMessageSizeEditor = new JBIntSpinner(10, 1, Integer.MAX_VALUE, 10);

    public ExecutionOptionsPanel() {
        super(new BorderLayout());

        final var formBuilder = FormBuilder.createFormBuilder();

        addLogQueries(formBuilder);

        addNormalizeQuery(formBuilder);

        addNotifications(formBuilder);

        addStrategies(formBuilder);

        formBuilder.addLabeledComponent("Show a warning when response is more than, Mb: ", warningMessageSizeEditor);

        add(formBuilder.getPanel());
    }

    private void addNotifications(FormBuilder formBuilder) {
        showConnectionState = new JBCheckBox("Show connection change notifications");

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.add(new JBLabel("Connection change timeout: "));
        p.add(connectionStateTimeout);

        formBuilder.addComponent(showConnectionState);
        formBuilder.setFormLeftIndent(20);
        formBuilder.addComponent(p);
        formBuilder.setFormLeftIndent(0);

        showConnectionState.addItemListener(e -> validateConnectionState());
    }

    private void addLogQueries(FormBuilder formBuilder) {
        final ContextHelpLabel infoLabel = ContextHelpLabel.create("Each executed query will be logged in daily file inside .kdbinb folder.");

        logQueries = new JBCheckBox("Log queries");

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(logQueries);
        p.add(Box.createHorizontalStrut(5));
        p.add(infoLabel);
        formBuilder.addComponent(p);
    }

    private void addNormalizeQuery(FormBuilder formBuilder) {
        final ContextHelpLabel infoLabel = ContextHelpLabel.create("A ';' letter will be added to the end of the current line if the next not empty starts from the begging.\n All system '\\l' call will be converted into system[\"...\"] calls.");

        normalizeQuery = new JBCheckBox("Normalize a query");

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(normalizeQuery);
        p.add(Box.createHorizontalStrut(5));
        p.add(infoLabel);
        formBuilder.addComponent(p);
    }

    private void addStrategies(FormBuilder formBuilder) {
        strategies.setEditable(false);
        strategies.setSelectedItem(EditorsBindingStrategy.MANUAL);

        final StringBuilder b = new StringBuilder("<html>");
        final int itemCount = strategies.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            final EditorsBindingStrategy itemAt = strategies.getItemAt(i);
            b.append("<b>").append(itemAt.getName()).append(": ").append("</b>").append(itemAt.getDescription());
            b.append("<br><br>");
        }
        b.append("</html>");
        final ContextHelpLabel infoLabel = ContextHelpLabel.create(b.toString());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(new JBLabel("Connection binding strategy: "));
        p.add(strategies);
        p.add(Box.createHorizontalStrut(5));
        p.add(infoLabel);

        formBuilder.addComponent(p);
    }


    public ExecutionOptions getOptions() {
        ExecutionOptions o = new ExecutionOptions();
        o.setBindingStrategy((EditorsBindingStrategy) strategies.getSelectedItem());
        o.setNormalizeQuery(normalizeQuery.isSelected());
        o.setShowConnectionChange(showConnectionState.isSelected());
        o.setConnectionChangeTimeout(connectionStateTimeout.getNumber());
        o.setWarningMessageMb(warningMessageSizeEditor.getNumber());
        o.setLogQueries(logQueries.isSelected());
        return o;
    }

    public void setOptions(ExecutionOptions options) {
        strategies.setSelectedItem(options.getBindingStrategy());
        normalizeQuery.setSelected(options.isNormalizeQuery());
        showConnectionState.setSelected(options.isShowConnectionChange());
        connectionStateTimeout.setNumber(options.getConnectionChangeTimeout());
        warningMessageSizeEditor.setNumber(options.getWarningMessageMb());
        logQueries.setSelected(options.isLogQueries());
        validateConnectionState();
    }

    private void validateConnectionState() {
        connectionStateTimeout.setEnabled(showConnectionState.isSelected());
    }
}
