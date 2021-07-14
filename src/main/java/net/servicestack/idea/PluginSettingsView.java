package net.servicestack.idea;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Layoric on 27/05/2016.
 */
public class PluginSettingsView {
    private JCheckBox optOutOfUsageCheckBox;
    private JPanel settingsPanel;

    public Boolean getOptOutOfUsage() {
        return optOutOfUsageCheckBox.isSelected();
    }

    public void setOptOutOfUsage(Boolean value) {
        optOutOfUsageCheckBox.setSelected(value);
    }

    public JPanel getSettingsPanel() {
        return this.settingsPanel;
    }

}
