package net.servicestack.idea.php;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpFileType;
import org.jetbrains.annotations.NotNull;

public class AddPhpAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Module module = getModule(anActionEvent);
        AddPhpRef dialog = new AddPhpRef(module); // Create your AddPhpRef dialog similar to your existing AddPythonRef
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setSize(dialog.getPreferredSize());
        dialog.setResizable(true);
        dialog.setTitle("Add PHP ServiceStack Reference");
        PsiElement element = LangDataKeys.PSI_ELEMENT.getData(anActionEvent.getDataContext());
        if (element instanceof PsiDirectory) {
            PsiDirectory selectedDir = (PsiDirectory)element;
            dialog.setSelectedDirectory(selectedDir.getVirtualFile().getPath());
            dialog.setFileName("dtos");
        }
        showDialog(dialog);
    }

    private void showDialog(AddPhpRef dialog) {
        try (var token = com.intellij.concurrency.ThreadContext.resetThreadContext()) {
            dialog.setVisible(true);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Module module = getModule(e);
        if (module == null) {
            e.getPresentation().setEnabled(false);
        }

        if (!isPhpModule(module)) { // Checking if this is a PHP project
            e.getPresentation().setVisible(false);
        }

        super.update(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    public boolean isPhpModule(Module module) {
        if (module == null) {
            return false;
        }

        // Retrieve the module's root manager
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

        // Retrieve the module's content roots
        VirtualFile[] roots = moduleRootManager.getContentRoots();

        // Iterate over each root file
        for (VirtualFile rootFile : roots) {
            VirtualFile[] children = rootFile.getChildren();

            // Iterate over each child file
            for (VirtualFile childFile : children) {
                // Check if a child file is 'composer.json' or 'index.php'
                String fileName = childFile.getName();
                if ("composer.json".equals(fileName) || "index.php".equals(fileName)) {
                    return true;
                }
            }
        }

        // If neither 'composer.json' nor 'index.php' are found, return false
        return false;
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

