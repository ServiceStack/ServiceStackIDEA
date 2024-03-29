package net.servicestack.idea.common;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by Layoric on 27/05/2016.
 */
public class PluginSettingsConfigurable implements SearchableConfigurable {

    private PluginSettingsView view;

    private PluginSettingsService settings;

    public PluginSettingsConfigurable() {

    }

    @NotNull
    @Override
    public String getId() {
        return "Settings.ServiceStackIDEA.Preview";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String s) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "ServiceStackIDEA Settings";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return getForm().getSettingsPanel();
    }


    @Override
    public boolean isModified() {
        return !getForm().getOptOutOfUsage().equals(settings.optOutOfStats);
    }

    @Override
    public void apply() throws ConfigurationException {
        if(settings == null)
            settings = PluginSettingsService.getInstance();
        settings.optOutOfStats = getForm().getOptOutOfUsage();
    }

    @Override
    public void reset() {
        if(settings == null)
            settings = PluginSettingsService.getInstance();
        getForm().setOptOutOfUsage(settings.optOutOfStats);
    }

    @Override
    public void disposeUIResources() {
        view = null;
    }

    @NotNull
    public PluginSettingsView getForm() {
        if (view == null) {
            view = new PluginSettingsView();
        }
        return view;
    }
}
