package org.kdb.inside.brains.view.treeview.forms;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.core.credentials.CredentialsError;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InstanceEditorDialog extends DialogWrapper {
    private Action testConnection;

    private boolean eventInFire = false;
    private String lastSymbolValue = "";

    private final InstanceOptionsPanel optionsEditor;
    private final CredentialsEditorPanel credentialsEditor;

    private final JBTextField nameField = new JBTextField();
    private final JBTextField hostField = new JBTextField();
    private final JBTextField symbolField = new JBTextField();
    private final IntegerField portField = new IntegerField("Port", 0, 65535);

    private final Mode mode;
    private final Project project;
    private final StructuralItem parent;
    private final KdbInstance defaultInstance;
    private final Predicate<String> nameValidator;

    private final JBLabel statusLabel = new JBLabel();

    private static final Color STATUS_ERROR_COLOR = JBColor.RED;
    private static final Color STATUS_CANCEL_COLOR = JBColor.GRAY;
    private static final Color STATUS_SUCCESS_COLOR = new JBColor(new Color(0x0E530D), new Color(0x082F07));

    public InstanceEditorDialog(@NotNull Mode mode, @NotNull Project project, @Nullable KdbInstance instance) {
        this(mode, project, instance == null ? null : instance.getParent() == null ? instance.getScope() : instance.getParent(), instance);
    }

    public InstanceEditorDialog(@NotNull Mode mode, @NotNull Project project, @Nullable KdbInstance instance, @NotNull Predicate<String> nameValidator) {
        this(mode, project, instance == null ? null : instance.getParent() == null ? instance.getScope() : instance.getParent(), instance, nameValidator);
    }

    public InstanceEditorDialog(@NotNull Mode mode, @NotNull Project project, @Nullable StructuralItem parent, @Nullable KdbInstance instance) {
        this(mode, project, parent, instance, new Predicate<>() {
            private final Set<String> busyNames = instance != null && instance.getParent() != null ? instance.getParent().getChildren().stream().filter(i -> i != instance).map(InstanceItem::getName).collect(Collectors.toSet()) : Set.of();

            @Override
            public boolean test(String s) {
                return !s.isBlank() && !busyNames.contains(s);
            }
        });
    }

    public InstanceEditorDialog(@NotNull Mode mode, @NotNull Project project, @Nullable StructuralItem parent, @Nullable KdbInstance instance, @NotNull Predicate<String> nameValidator) {
        super(project, false, IdeModalityType.PROJECT);
        this.mode = mode;
        this.project = project;
        this.nameValidator = nameValidator;
        this.parent = parent;

        defaultInstance = instance;

        final KdbScope scope = parent == null ? null : parent.getScope();

        optionsEditor = new InstanceOptionsPanel(scope);
        optionsEditor.setInstanceOptions(instance != null ? instance.getOptions() : null);

        credentialsEditor = new CredentialsEditorPanel(scope);

        initPanel(scope, instance);
    }

    private void initPanel(@Nullable KdbScope scope, @Nullable KdbInstance instance) {
        init();

        portField.setCanBeEmpty(false);
        symbolField.requestFocusInWindow();

        if (mode == Mode.CREATE || mode == Mode.FAKE) {
            setTitle("Create Kdb Instance");
            setOKButtonText("Create");
        } else {
            setTitle("Modify Kdb Instance");
            setOKButtonText("Update");
        }

        if (instance == null) {
            String contents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
            if (contents != null && !contents.isBlank() && contents.indexOf(':') != -1) {
                instance = KdbInstance.parseInstance(contents);
            }
        }

        if (instance != null) {
            nameField.setText(instance.getName());
            hostField.setText(instance.getHost());
            portField.setValue(instance.getPort());
            symbolField.setText("`:" + hostField.getText() + ":" + portField.getText());
            credentialsEditor.setCredentials(instance.getCredentials());
        }

        lastSymbolValue = symbolField.getText();
        if (mode == Mode.FAKE) {
            nameField.setText(lastSymbolValue);
        }

        optionsEditor.addOptionsChangedListener(options -> validateOk());
        credentialsEditor.addCredentialChangeListener(options -> validateOk());

        symbolField.getDocument().addDocumentListener(new TheDocumentListener() {
            @Override
            void changed() {
                String text = symbolField.getText().strip();
                if (!text.isEmpty() && text.charAt(0) == '`') {
                    text = text.substring(1);
                }
                if (!text.isEmpty() && text.charAt(0) == ':') {
                    text = text.substring(1);
                }

                if (text.isEmpty()) {
                    hostField.setText("");
                    portField.setText("");
                    return;
                }

                final int p = text.indexOf(':');
                final int c = text.indexOf(':', p + 1);

                if (p > 0) {
                    hostField.setText(text.substring(0, p));
                } else {
                    hostField.setText(text);
                }

                if (c > 0) {
                    portField.setText(text.substring(p + 1, c));
                    credentialsEditor.setCredentials(text.substring(c + 1));
                } else {
                    if (p < 0) {
                        portField.setText("");
                    } else {
                        portField.setText(text.substring(p + 1));
                    }
                    credentialsEditor.setCredentials(null);
                }

                invalidateName();
                validateOk();
            }
        });

        nameField.getDocument().addDocumentListener(new TheDocumentListener() {
            @Override
            void changed() {
                validateOk();
            }
        });

        final TheDocumentListener symbolGenerator = new TheDocumentListener() {
            @Override
            void changed() {
                symbolField.setText("`:" + hostField.getText() + ":" + portField.getText());
                invalidateName();
                validateOk();
            }
        };

        hostField.getDocument().addDocumentListener(symbolGenerator);
        portField.getDocument().addDocumentListener(symbolGenerator);

        validateOk();
    }

    private void invalidateName() {
        final String text = symbolField.getText();
        if (nameField.getText().equals(lastSymbolValue)) {
            nameField.setText(text);
        }
        lastSymbolValue = text;
    }

    @Nullable
    @Override
    protected JPanel createSouthAdditionalPanel() {
        final JPanel p = new NonOpaquePanel(new FlowLayout());
        p.add(createButtonsPanel(Collections.singletonList(createJButtonForAction(testConnection))));
        p.add(statusLabel);
        return p;
    }

    private void clearTestStatus() {
        statusLabel.setText("");
        statusLabel.setForeground(JBColor.BLACK);
    }

    private void doTestConnection() {
        clearTestStatus();

        final KdbConnectionManager instance = KdbConnectionManager.getManager(project);
        try {
            final InstanceState test = instance.test(createInstance());
            switch (test) {
                case CONNECTING:
                    statusLabel.setText("Connecting to the instance...");
                    break;
                case CONNECTED:
                    statusLabel.setForeground(STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Connected successfully");
                    break;
                case DISCONNECTED:
                    statusLabel.setForeground(STATUS_CANCEL_COLOR);
                    statusLabel.setText("Connection cancelled");
                    break;
            }
        } catch (Exception ex) {
            statusLabel.setText(ex.getMessage());
            statusLabel.setForeground(STATUS_ERROR_COLOR);
        }
    }

    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        final List<ValidationInfo> res = new ArrayList<>();
        if (nameField.getText().isBlank()) {
            res.add(new ValidationInfo("Please provide the instance name", nameField));
        }

        if (hostField.getText().isBlank()) {
            res.add(new ValidationInfo("Please provide hostname", hostField));
        }

        if (portField.getValue() == 0) {
            res.add(new ValidationInfo("Please provide hostname", hostField));
        }

        final List<CredentialsError> validationInfo = credentialsEditor.validateEditor();
        if (validationInfo != null) {
            res.addAll(validationInfo.stream().map(i -> new ValidationInfo(i.message(), i.component())).toList());
        }
        return res;
    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        testConnection = new AbstractAction("Check Instance") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doTestConnection();
            }
        };
    }

    private void validateOk() {
        clearTestStatus();

        boolean enabled = true;

        if (portField.getValue() == 0) {
            setErrorText("Port can't be empty", portField);
            enabled = false;
        } else {
            setErrorText(null, portField);
        }

        if (hostField.getText().isBlank()) {
            setErrorText("Host can't be empty", hostField);
            enabled = false;
        } else {
            setErrorText(null, hostField);
        }

        // Has host/port - that's ok
        testConnection.setEnabled(enabled);

        if (!nameValidator.test(nameField.getText())) {
            setErrorText("Invalid name", nameField);
            enabled = false;
        } else {
            setErrorText(null, nameField);
        }

        if (!enabled) {
            setOKActionEnabled(false);
        } else {
            if (defaultInstance == null) {
                setOKActionEnabled(true);
            } else {
                final KdbInstance instance = createInstance();
                setOKActionEnabled(!Objects.equals(defaultInstance.toQualifiedSymbol(), instance.toQualifiedSymbol()) || !defaultInstance.getName().equals(instance.getName()));
            }
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        final JPanel leftPanel = createLeftPanel();
        final JPanel rightPanel = createRightPanel();

        final BorderLayoutPanel leftC = new BorderLayoutPanel();
        leftC.addToTop(leftPanel);

        final BorderLayoutPanel rightC = new BorderLayoutPanel();
        rightC.addToTop(rightPanel);

        final DialogPanel mainPanel = new DialogPanel(new GridLayout(1, 2, 10, 0));
        mainPanel.add(leftC);
        mainPanel.add(rightC);
        mainPanel.setPreferredFocusedComponent(symbolField);

        return mainPanel;
    }

    @NotNull
    private JPanel createRightPanel() {
        credentialsEditor.addCredentialChangeListener(credentials -> {
            if (!eventInFire) {
                eventInFire = true;
                try {
                    if (credentials == null) {
                        symbolField.setText("`:" + hostField.getText() + ":" + portField.getText());
                    } else {
                        symbolField.setText("`:" + hostField.getText() + ":" + portField.getText() + ":" + credentialsEditor.getViewableCredentials());
                    }
                    invalidateName();
                } finally {
                    eventInFire = false;
                }
            }
        });

        final FormBuilder formBuilder = FormBuilder.createFormBuilder();
        formBuilder
                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Credentials"))
                .setFormLeftIndent(20)
                .addComponent(credentialsEditor)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Options"))
                .setFormLeftIndent(20)
                .addComponent(optionsEditor)

                .addComponentFillVertically(new JPanel(), 0);

        return formBuilder.getPanel();
    }

    @NotNull
    private JPanel createLeftPanel() {
        final GridBag c = new GridBag()
                .setDefaultAnchor(0, GridBagConstraints.LINE_START)
                .setDefaultAnchor(1, GridBagConstraints.CENTER)
                .setDefaultWeightX(1, 1)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultInsets(3, 10, 3, 3);

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new TitledSeparator("Name"), c.nextLine().next().coverLine(2).insetLeft(0));

        panel.add(new JBLabel("Path: "), c.nextLine().next());
        panel.add(new JBLabel(parent == null ? "" : parent.getCanonicalName()), c.next());

        panel.add(new JBLabel("Name: "), c.nextLine().next());
        panel.add(nameField, c.next());

        panel.add(new TitledSeparator("Connection"), c.nextLine().next().coverLine(2).insetLeft(0));

        panel.add(new JBLabel("Symbol: "), c.nextLine().next());
        panel.add(symbolField, c.next());

        panel.add(new JBLabel("Host: "), c.nextLine().next());

        final JPanel p = new JPanel(new BorderLayout(3, 0));
        hostField.setMinimumSize(new Dimension(300, 0));
        p.add(hostField, BorderLayout.CENTER);

        final JPanel p2 = new JPanel(new BorderLayout(3, 0));
        p2.add(new JLabel("Port:"), BorderLayout.LINE_START);

        portField.setPreferredSize(new Dimension(50, 0));
        p2.add(portField, BorderLayout.LINE_END);

        p.add(p2, BorderLayout.LINE_END);

        panel.add(p, c.next());
/*
        panel.add(new TitledSeparator("Advanced"), c.nextLine().next().coverLine(2).insetLeft(0));
        panel.add(Box.createHorizontalBox(), c.nextLine().next());
        panel.add(new JBCheckBox("Asynchronous connection"), c.next());
*/

        return panel;
    }

    public KdbInstance createInstance() {
        final String credentials = credentialsEditor.getCredentials();
        final InstanceOptions options = optionsEditor.getInstanceOptions();
        return new KdbInstance(nameField.getText().strip(), hostField.getText().strip(), portField.getValue(), credentials, options);
    }

    private abstract class TheDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            changedImpl();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            changedImpl();
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            changedImpl();
        }

        private void changedImpl() {
            if (!eventInFire) {
                eventInFire = true;
                try {
                    changed();
                } finally {
                    eventInFire = false;
                }
            }
        }

        abstract void changed();
    }

    public enum Mode {
        CREATE,
        UPDATE,
        FAKE
    }
}
