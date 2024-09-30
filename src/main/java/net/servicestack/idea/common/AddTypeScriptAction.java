package net.servicestack.idea.common;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Layoric on 28/05/2016.
 */
public class AddTypeScriptAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Module module = getModule(anActionEvent);
        AddTypeScriptRef dialog = new AddTypeScriptRef(module);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setSize(dialog.getPreferredSize());
        dialog.setResizable(true);
        dialog.setTitle("Add TypeScript ServiceStack Reference");
        PsiElement element = LangDataKeys.PSI_ELEMENT.getData(anActionEvent.getDataContext());
        if (element instanceof PsiDirectory) {
            PsiDirectory selectedDir = (PsiDirectory)element;
            dialog.setSelectedDirectory(selectedDir.getVirtualFile().getPath());
            String initialName = "dtos";
            dialog.setFileName(initialName);
        }
        showDialog(dialog);
    }

    private void showDialog(AddTypeScriptRef dialog) {
        dialog.setVisible(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Module module = getModule(e);
        if (module == null) {
            e.getPresentation().setEnabled(false);
        }
        // If the plugin is installed, make visible.
        // since Typescript/web is common development
        // to variable languages/platforms.
        e.getPresentation().setVisible(true);

        super.update(e);
    }

    static Module getModule(Project project) {
        if (project == null)
            return null;
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length > 0) {
            return modules[0];
        }
        return null;
    }

    static Module getModule(AnActionEvent e) {
        Module module = e.getData(LangDataKeys.MODULE);
        if (module == null) {
            Project project = e.getData(LangDataKeys.PROJECT);
            return getModule(project);
        } else {
            return module;
        }
    }
}
