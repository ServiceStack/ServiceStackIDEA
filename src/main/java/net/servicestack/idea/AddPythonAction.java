package net.servicestack.idea;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.facet.PythonFacet;
import com.jetbrains.python.facet.PythonFacetSettings;
import net.servicestack.idea.common.INativeTypesHandler;
import org.jetbrains.annotations.NotNull;

public class AddPythonAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Module module = getModule(anActionEvent);
        AddPythonRef dialog = new AddPythonRef(module);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setSize(dialog.getPreferredSize());
        dialog.setResizable(true);
        dialog.setTitle("Add Python ServiceStack Reference");
        PsiElement element = LangDataKeys.PSI_ELEMENT.getData(anActionEvent.getDataContext());
        INativeTypesHandler defaultTsNativeTypesHandler = new PythonNativeTypesHandler();
        if (element instanceof PsiDirectory) {
            PsiDirectory selectedDir = (PsiDirectory)element;
            dialog.setSelectedDirectory(selectedDir.getVirtualFile().getPath());
            dialog.setFileName("dtos");
        }
        showDialog(dialog);
    }

    private void showDialog(AddPythonRef dialog) {
        dialog.setVisible(true);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Module module = getModule(e);
        if (module == null) {
            e.getPresentation().setEnabled(false);
        }

        if (!isPythonModule(module)) { // Checking if this is a python project for example
            e.getPresentation().setVisible(false);
        }

        super.update(e);
    }

    public boolean isPythonModule(Module module) {
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
                // Check if a child file is a Python file
                if (childFile.getFileType() instanceof PythonFileType) {
                    return true;
                }
            }
        }

        // If no Python files are found, return false
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