package net.servicestack.idea;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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
            String initialName = getInitialFileName(selectedDir.getVirtualFile().getPath(),defaultTsNativeTypesHandler);
            dialog.setInitialDtoName(initialName);
        }
        showDialog(dialog);
    }

    private void showDialog(AddPythonRef dialog) {
        dialog.setVisible(true);
    }

    private String getInitialFileName(String path, INativeTypesHandler defaultTsNativeTypesHandler) {
        String initName = "dtos";
        File existingFile = new File(path + "/" + initName +
                defaultTsNativeTypesHandler.getFileExtension());
        if(!existingFile.exists())
            return initName;
        int count = 1;
        while(true) {
            existingFile = new File(path + "/" + initName + count +
                    defaultTsNativeTypesHandler.getFileExtension());
            if(existingFile.exists()) {
                count++;
            } else {
                break;
            }
        }
        return initName + count;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Module module = getModule(e);
        if (module == null) {
            e.getPresentation().setEnabled(false);
        }

        if (!PlatformUtils.isPyCharm()) {
            e.getPresentation().setVisible(false);
        }

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