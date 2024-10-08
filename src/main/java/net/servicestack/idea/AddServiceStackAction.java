package net.servicestack.idea;

import com.google.common.collect.Lists;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddServiceStackAction extends AnAction {

    public void actionPerformed(@NotNull AnActionEvent e) {
        Module module = getModule(e);
        AddRef dialog = new AddRef(module,e);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setSize(dialog.getPreferredSize());
        dialog.setResizable(true);
        dialog.setTitle("Add ServiceStack Reference");

        if(GradleBuildFileHelper.isUsingKotlin(e)) {
            dialog.setDefaultNativeTypesHandler(new KotlinNativeTypesHandler());
        } else if(GradleBuildFileHelper.isDartProject(module)) {
            dialog.setDefaultNativeTypesHandler(new DartNativeTypesHandler());
        } else {
            dialog.setDefaultNativeTypesHandler(new JavaNativeTypesHandler());
        }

        //Check if a package was selected in the left hand menu, populate package name
        PsiElement element = LangDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        if (element instanceof PsiPackage) {
            PsiPackage psiPackage = (PsiPackage) element;
            dialog.setSelectedPackage(psiPackage);
        }

        //Check if a directory containing a Java file was selected, populate package name
        if (element instanceof PsiDirectory) {
            PsiElement firstChild = element.getFirstChild();
            dialog.setSelectedDirectory(((PsiDirectory) element).getVirtualFile().getPath());
            if (firstChild instanceof PsiJavaFile) {
                PsiJavaFile firstJavaFile = (PsiJavaFile) firstChild;
                PsiPackage mainPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(firstJavaFile.getPackageName());
                if (mainPackage != null) {
                    dialog.setSelectedPackage(mainPackage);
                }
            }

            ShowDialog(module, dialog, e);
            return;
        }

        //Check if a Java file was selected, display without a package name if no file.
        VirtualFile selectedFile = LangDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (selectedFile == null) {
            //noinspection UnresolvedPluginConfigReference
            Notification notification = new Notification("ServiceStackIDEA", "Error Add ServiceStack Reference", "Context menu failed find folder or file.", NotificationType.ERROR);
            Notifications.Bus.notify(notification);
            return;
        }
        if (selectedFile.isDirectory()) {
            dialog.setSelectedDirectory(selectedFile.getPath());
        } else if (selectedFile.getParent().isDirectory()) {
            dialog.setSelectedDirectory(selectedFile.getParent().getPath());
        } else {
            //noinspection UnresolvedPluginConfigReference
            Notification notification = new Notification("ServiceStackIDEA", "Error Add ServiceStack Reference", "Context menu failed find folder or file.", NotificationType.ERROR);
            Notifications.Bus.notify(notification);
            return;
        }

        //Check for document, display without a package name if no document.
        Document document = FileDocumentManager.getInstance().getDocument(selectedFile);
        if (document == null) {
            ShowDialog(module, dialog,e);
            return;
        }

        //Check if a 'PsiFile', display without a package name if no PsiFile.
        PsiFile psiFile = PsiDocumentManager.getInstance(module.getProject()).getPsiFile(document);
        if (psiFile == null) {
            ShowDialog(module, dialog,e);
            return;
        }

        //Finally check if a Java file and populate package name with class package name.
        if (psiFile.getFileType().getName().equals("JAVA")) {
            PsiJavaFile javaFile = (PsiJavaFile) psiFile;
            PsiPackage mainPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(javaFile.getPackageName());
            if (mainPackage != null) {
                dialog.setSelectedPackage(mainPackage);
            }
        }
        ShowDialog(module, dialog,e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private void ShowDialog(Module module, AddRef dialog, AnActionEvent event) {
        if (GradleBuildFileHelper.isGradleModule(event) && GradleBuildFileHelper.isUsingKotlin(event)) {
            dialog.setFileName("dtos.kt");
        }
        else if (IDEAPomFileHelper.isMavenProjectWithKotlin(module)) {
            dialog.setFileName("dtos.kt");
        }
        else if (GradleBuildFileHelper.isDartProject(module)) {
            dialog.setFileName("dtos.dart");
        }
        try (var token = com.intellij.concurrency.ThreadContext.resetThreadContext()) {
            dialog.setVisible(true);
        }
    }

    private PsiPackage testPackage(Module module, String packageName, List<String> packageArray) {
        List<String> packageNameOrderedList = Lists.reverse(packageArray);
        for (int i = 0; i < packageNameOrderedList.size(); i++) {
            if (i < packageNameOrderedList.size() - 1) {
                packageName += packageNameOrderedList.get(i) + ".";
            } else {
                packageName += packageNameOrderedList.get(i);
            }
        }
        return JavaPsiFacade.getInstance(module.getProject()).findPackage(packageName);
    }

    @Override
    public void update(AnActionEvent e) {
        Module module = getModule(e);
        if(module == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        if (!(isJavaProject(module) || isAndroidProject(module) || isDartProject(module))) {
            e.getPresentation().setVisible(false);
            return;
        }

        boolean isMavenModule =  IDEAPomFileHelper.isMavenModule(module);

        if (isAndroidProject(module) ||
                isMavenModule ||
                GradleBuildFileHelper.isGradleModule(e) ||
                isDartProject(module) ||
                IsKotlinProject(e)) {
            e.getPresentation().setEnabled(true);
        } else {
            e.getPresentation().setEnabled(false);
            return;
        }

        super.update(e);
    }

    public boolean isJavaProject(Module module) {
        if (module == null) {
            return false;
        }

        LanguageLevelModuleExtension languageLevelModuleExtension = ModuleRootManager.getInstance(module).getModuleExtension(LanguageLevelModuleExtension.class);

        LanguageLevel languageLevel = null;

        if (languageLevelModuleExtension != null) {
            languageLevel = languageLevelModuleExtension.getLanguageLevel();
        }

        if (languageLevel == null) { // module does not have a specific language level, let's check global project setting
            LanguageLevelProjectExtension languageLevelProjectExtension = LanguageLevelProjectExtension.getInstance(module.getProject());
            if (languageLevelProjectExtension != null) {
                languageLevel = languageLevelProjectExtension.getLanguageLevel();
            }
        }

        // Check if language is not null and at least JDK_1_1 (or your required Java level.)
        if (languageLevel != null && languageLevel.isAtLeast(LanguageLevel.JDK_1_3)) {
            return true;
        }

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

    private static boolean isAndroidProject(@NotNull Module module) {
        Facet[] facetsByType = FacetManager.getInstance(module).getAllFacets();
        for (Facet facet : facetsByType) {
            if (facet.getTypeId().toString().equals("android")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDartProject(@NotNull Module module) {
        return GradleBuildFileHelper.isDartProject(module);
    }

    private static boolean IsKotlinProject(AnActionEvent event) {
        return GradleBuildFileHelper.isUsingKotlin(event);
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
