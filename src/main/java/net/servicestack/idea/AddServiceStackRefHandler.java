package net.servicestack.idea;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import net.servicestack.idea.common.Analytics;
import net.servicestack.idea.common.DialogErrorMessages;
import net.servicestack.idea.common.IDEAUtils;
import net.servicestack.idea.common.INativeTypesHandler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.servicestack.idea.common.IDEAUtils.*;


public class AddServiceStackRefHandler {
    public static void handleOk(String addressUrl, String qualifiedPackageName,
                                String fileName, String selectedDirectory,
                                Module module, AnActionEvent event,
                                INativeTypesHandler nativeTypesHandler,
                                StringBuilder errorMessage) {
        List<String> javaCodeLines = getDtoLines(addressUrl,
                qualifiedPackageName,
                fileName,
                nativeTypesHandler,
                errorMessage);
        if (javaCodeLines == null) return;

        boolean showDto = true;
        boolean isMavenModule = IDEAPomFileHelper.isMavenModule(module);
        if(isMavenModule) {
            showDto = !tryAddMavenDependency(module);
        } else {
            //Gradle
            try {
                showDto = !addGradleDependencyIfRequired(module,event);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                String message = "Failed to update build.gradle with '" +
                        DepConfig.getClientVersionString() +
                        "'. " + e.getLocalizedMessage();
                //noinspection UnresolvedPluginConfigReference
                Notification notification = new Notification(
                        "ServiceStackIDEA",
                        "Warning Add ServiceStack Reference",
                        message,
                        NotificationType.WARNING);
                Notifications.Bus.notify(notification);
            }
        }

        String dtoPath;
        try {
            dtoPath = getDtoPath(module,
                    qualifiedPackageName,
                    selectedDirectory,
                    fileName,
                    nativeTypesHandler,
                    errorMessage);
        } catch (Exception e) {
            return;
        }

        if (!IDEAUtils.writeDtoFile(javaCodeLines, dtoPath, errorMessage)) {
            return;
        }
        Analytics.SubmitAnonymousAddReferenceUsage(nativeTypesHandler);
        IDEAUtils.refreshFile(module, dtoPath, showDto);
        VirtualFileManager.getInstance().syncRefresh();
    }

    @Nullable
    private static List<String> getDtoLines(String addressUrl,
                                            String qualifiedPackageName,
                                            String fileName,
                                            INativeTypesHandler nativeTypesHandler,
                                            StringBuilder errorMessage) {
        Map<String,String> options = new HashMap<>();
        List<String> javaCodeLines;
        try {
            options.put("Package", qualifiedPackageName);
            String name = getDtoNameWithoutExtension(fileName, nativeTypesHandler)
                    .replaceAll("\\.", "_")
                    .replaceAll("-","_");
            options.put("GlobalNamespace", name);
            javaCodeLines = nativeTypesHandler.getUpdatedCode(addressUrl, options);

            if (!javaCodeLines.get(0).startsWith("/* Options:")) {
                //Invalid endpoint
                errorMessage.append("The address url is not a valid ServiceStack endpoint.");
                return null;
            }
        }
        catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
            DialogErrorMessages.appendInvalidEndpoint(errorMessage, addressUrl, e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            DialogErrorMessages.appendReadResponseError(errorMessage, addressUrl, e);
            return null;
        }
        return javaCodeLines;
    }

    private static boolean tryAddMavenDependency(Module module) {
        boolean showDto;
        String message = "Unable to locate module pom.xml file. Can't add required dependency '" +
                DepConfig.getClientVersionString() +
                "'.";
        //noinspection UnresolvedPluginConfigReference
        Notification notification = new Notification(
                "ServiceStackIDEA",
                "Warning Add ServiceStack Reference",
                message,
                NotificationType.WARNING);
        try {
            IDEAPomFileHelper pomFileHelper = new IDEAPomFileHelper();
            String pomFilePath = IDEAPomFileHelper.findNearestModulePomFile(module);
            if (pomFilePath == null) {
                Notifications.Bus.notify(notification);
                return false;
            }
            File pomLibFile = new File(pomFilePath);
            showDto = pomFileHelper.addMavenDependency(module,pomLibFile, DepConfig.servicestackGroupId, DepConfig.clientPackageId, DepConfig.servicestackVersion);
            if (pomFileHelper.addMavenDependency(module,pomLibFile, DepConfig.gsonGroupId, DepConfig.gsonPackageId, DepConfig.gsonVersion))
                showDto = true;

            IDEAUtils.refreshFile(module,pomFilePath,showDto);
        } catch(Exception e) {
            showDto = false;
            e.printStackTrace();
            //noinspection UnresolvedPluginConfigReference
            notification = new Notification(
                    "ServiceStackIDEA",
                    "Warning Add ServiceStack Reference",
                    "Unable to add maven dependency. " + e.getLocalizedMessage(),
                    NotificationType.WARNING);
            Notifications.Bus.notify(notification);
        }
        return showDto;
    }

    private static boolean addGradleDependencyIfRequired(Module module,AnActionEvent event) throws FileNotFoundException {
        boolean isAndroid = GradleBuildFileHelper.isAndroidProject(module);
        boolean depAdded = GradleBuildFileHelper.addDependency(event, DepConfig.servicestackGroupId,
                isAndroid ? DepConfig.androidPackageId : DepConfig.clientPackageId,
                DepConfig.servicestackVersion);
        if (GradleBuildFileHelper.addDependency(event, DepConfig.gsonGroupId, DepConfig.gsonPackageId, DepConfig.gsonVersion))
            depAdded = true;

        if (depAdded) {
            IDEAUtils.refreshBuildFile(module);
            return true;
        }
        return false;
    }

    private static String getDtoPath(Module module,
                                     String qualifiedPackageName,
                                     String selectedDirectory,
                                     String fileName,
                                     INativeTypesHandler nativeTypesHandler,
                                     StringBuilder errorMessage) throws FileNotFoundException {
        String projectBasePath = module.getProject().getBasePath();
        if(projectBasePath == null) {
            throw new FileNotFoundException("Module file not found. Unable to add DTO to project.");
        }
        File projectBase = new File(projectBasePath);
        String fullDtoPath;

        PsiPackage mainPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(qualifiedPackageName);
        if (mainPackage != null && mainPackage.isValid() && mainPackage.getDirectories().length > 0) {
            File file = new File(selectedDirectory);
            VirtualFile selectedFolder = LocalFileSystem.getInstance().findFileByIoFile(file);
            if (selectedFolder == null) {
                errorMessage.append("Unable to determine path for DTO file.");
                throw new FileNotFoundException();
            }
            PsiDirectory rootPackageDir = PsiManager.getInstance(module.getProject()).findDirectory(selectedFolder);
            if (rootPackageDir == null) {
                errorMessage.append("Unable to determine path for DTO file.");
                throw new FileNotFoundException();
            }
            fullDtoPath = rootPackageDir.getVirtualFile().getPath() +
                    File.separator +
                    getDtoFileName(fileName, nativeTypesHandler);
        } else {
            String moduleSourcePath;
            if(projectBase.getParent() == null) {
                moduleSourcePath = projectBase.getPath() + "/main/java";
            } else {
                moduleSourcePath = projectBase.getParent() + "/src/main/java";
            }
            fullDtoPath = moduleSourcePath + File.separator +
                    getDtoFileName(fileName, nativeTypesHandler);
        }
        return fullDtoPath;
    }
}
