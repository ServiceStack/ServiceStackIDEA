package net.servicestack.idea;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.JBColor;
import net.servicestack.idea.common.IDEAUtils;
import net.servicestack.idea.common.INativeTypesHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class AddRef extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextPane errorTextPane;
    private JTextField addressUrlTextField;
    private TextFieldWithBrowseButton packageBrowse;
    private JTextField nameTextField;
    private JTextPane infoTextPane;
    private Module module;
    private AnActionEvent sourceEvent;

    private String errorMessage;
    private String selectedDirectory;

    private INativeTypesHandler defaultNativeTypesHandler;

    public AddRef(@NotNull Module module, AnActionEvent sourceEvent) {
        this.module = module;
        this.sourceEvent = sourceEvent;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        ImageIcon imageIcon = IDEAUtils.createImageIcon("/servicestack-64.png", "ServiceStack");
        if (imageIcon != null) {
            this.setIconImage(imageIcon.getImage());
        }
        errorTextPane.setForeground(JBColor.RED);

        buttonOK.setEnabled(false);

        addressUrlTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = null;
                if (input instanceof JTextField) {
                    text = ((JTextField) input).getText();
                }

                return text != null && text.length() > 0;
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                boolean valid = verify(input);
                if (!valid) {
                    errorMessage = "URL Address is required";
                    errorTextPane.setVisible(true);
                    errorTextPane.setText(errorMessage);
                }

                return true;
            }
        });
        nameTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = null;
                if (input instanceof JTextField) {
                    text = ((JTextField) input).getText();
                }

                return text != null && text.length() > 0;
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                boolean valid = verify(input);
                if (!valid) {
                    errorMessage = "A file name is required.";
                    errorTextPane.setVisible(true);
                    errorTextPane.setText(errorMessage);
                }

                return true;
            }
        });

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                buttonOK.setEnabled(
                    nameTextField.getInputVerifier().verify(nameTextField) &&
                    addressUrlTextField.getInputVerifier().verify(addressUrlTextField)
                );
            }
        });

        addressUrlTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                buttonOK.setEnabled(
                    nameTextField.getInputVerifier().verify(nameTextField) &&
                    addressUrlTextField.getInputVerifier().verify(addressUrlTextField)
                );
            }
        });

        buttonOK.addActionListener(e -> processOK());

        packageBrowse.addActionListener(new BrowsePackageListener(packageBrowse, module.getProject(), "Browse packages"));

        buttonCancel.addActionListener(e -> onCancel());

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void setSelectedPackage(@NotNull PsiPackage selectedPackage) {
        setPackageBrowseText(selectedPackage.getQualifiedName());
    }

    public void setFileName(@NotNull String fileName) {
        nameTextField.setText(fileName);
    }

    public void setDefaultNativeTypesHandler(INativeTypesHandler nativeTypesHandler) {
        defaultNativeTypesHandler = nativeTypesHandler;
    }

    public void setSelectedDirectory(@NotNull String selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
    }

    private void processOK() {
        buttonOK.setEnabled(false);
        buttonCancel.setEnabled(false);
        errorMessage = null;
        errorTextPane.setVisible(false);

        URL serviceUrl;
        try {
            serviceUrl = new URL("https://api.github.com/repos/ServiceStack/ServiceStack.Java/tags");
            URLConnection javaResponseConnection = serviceUrl.openConnection();
            StringBuilder builder = new StringBuilder();
            BufferedReader javaResponseReader = new BufferedReader(
                    new InputStreamReader(
                            javaResponseConnection.getInputStream()));
            String metadataInputLine;

            while ((metadataInputLine = javaResponseReader.readLine()) != null)
                builder.append(metadataInputLine);


            JsonElement jElement = JsonParser.parseString(builder.toString());
            if (jElement.getAsJsonArray().size() > 0) {
                String latestTag = jElement.getAsJsonArray().get(0).getAsJsonObject().get("name").getAsJsonPrimitive().getAsString();
                DepConfig.setServiceStackVersion(latestTag.substring(1));
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            //noinspection UnresolvedPluginConfigReference
            Notification notification = new Notification(
                    "ServiceStackIDEA",
                    "Warning add serviceStack reference",
                    "Unable to get latest version of required dependencies, falling back to known available version.\n" +
                            "Please check the JCenter/Maven Central for the latest published versions of the ServiceStack java clients and update your dependencies.",
                    NotificationType.WARNING);
            Notifications.Bus.notify(notification);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                onOK();
            } catch (Exception e1) {
                //noinspection UnresolvedPluginConfigReference
                Notification notification = new Notification(
                        "ServiceStackIDEA",
                        "Error adding ServiceStack reference",
                        e1.getLocalizedMessage(),
                        NotificationType.ERROR);
                Notifications.Bus.notify(notification);
                e1.printStackTrace();
                errorMessage = errorMessage != null ? errorMessage : "An error occurred adding reference - " + e1.getMessage();
            }
            if (errorMessage != null) {
                errorTextPane.setVisible(true);
                errorTextPane.setText(errorMessage);
            }
            buttonOK.setEnabled(true);
            buttonCancel.setEnabled(true);
        });

    }

    private void setPackageBrowseText(String packageName) {
        packageBrowse.setText(packageName);
    }

    private ImageIcon createImageIcon(String path, String description) {
        URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    private void onOK() {
        StringBuilder errorMessage = new StringBuilder();
        AddServiceStackRefHandler.handleOk(
                addressUrlTextField.getText(),
                packageBrowse.getText(),
                nameTextField.getText(),
                selectedDirectory,
                module,
                sourceEvent,
                this.defaultNativeTypesHandler,
                errorMessage);

        if (errorMessage.toString().length() > 0) {
            errorTextPane.setText(errorMessage.toString());
            errorTextPane.setVisible(true);
        } else {
            dispose();
        }
    }

    private class BrowsePackageListener implements ActionListener {
        private TextFieldWithBrowseButton _textField;
        private Project _project;
        private String _title;

        public BrowsePackageListener(TextFieldWithBrowseButton textField, Project project, String title) {
            _textField = textField;
            _project = project;
            _title = title;
        }

        public void actionPerformed(ActionEvent e) {
            PackageChooserDialog dialog = new PackageChooserDialog(_title, _project);
            dialog.selectPackage(_textField.getText());
            dialog.show();

            if (dialog.getExitCode() == PackageChooserDialog.CANCEL_EXIT_CODE) {
                return;
            }
            _textField.setText(dialog.getSelectedPackage().getQualifiedName());
        }
    }

    private void onCancel() {
        dispose();
    }
}
