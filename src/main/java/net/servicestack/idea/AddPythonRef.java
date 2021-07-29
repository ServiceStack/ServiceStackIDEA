package net.servicestack.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Locale;

public class AddPythonRef extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField addressUrlTextField;
    private JTextField nameTextField;
    private JTextPane infoTextPane;
    private JTextPane errorTextPane;

    private String selectedDirectory;
    private String errorMessage;

    private Module module;

    public AddPythonRef(Module module) {
        this.module = module;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        ImageIcon imageIcon = createImageIcon();
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
                buttonOK.setEnabled(nameTextField.getInputVerifier().verify(nameTextField) && addressUrlTextField.getInputVerifier().verify(addressUrlTextField));
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
                buttonOK.setEnabled(nameTextField.getInputVerifier().verify(nameTextField) && addressUrlTextField.getInputVerifier().verify(addressUrlTextField));
            }
        });

        buttonOK.addActionListener(e -> onOK());

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

    private void onOK() {
        buttonOK.setEnabled(false);
        StringBuilder errorMessageBuilder = new StringBuilder();
        try {
            AddPythonRefHandler.handleOk(
                    this.module,
                    this.addressUrlTextField.getText(),
                    this.nameTextField.getText(),
                    this.selectedDirectory,
                    errorMessageBuilder
            );
            if (errorMessageBuilder.toString().length() > 0) {
                errorTextPane.setText(errorMessageBuilder.toString());
                errorTextPane.setVisible(true);
            } else {
                dispose();
            }
        } catch (Exception e) {
            errorMessageBuilder.append("An unexpected error has occurred. - ");
            errorMessageBuilder.append(e.getMessage());
            e.printStackTrace();
        } finally {
            if (errorMessageBuilder.toString().length() > 0) {
                errorTextPane.setText(errorMessageBuilder.toString());
                errorTextPane.setVisible(true);
                buttonOK.setEnabled(true);
            }
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        AddPythonRef dialog = new AddPythonRef(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    private ImageIcon createImageIcon() {
        URL imgURL = getClass().getResource("/logo-16.png");
        if (imgURL != null) {
            return new ImageIcon(imgURL, "ServiceStack");
        } else {
            System.err.println("Couldn't find file: " + "/logo-16.png");
            return null;
        }
    }

    public void setSelectedDirectory(String selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
    }

    public void setFileName(String initialDtoName) {
        this.nameTextField.setText(initialDtoName);
    }


}
