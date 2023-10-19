package net.servicestack.idea.php;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.php.composer.ComposerDataService;
import com.jetbrains.php.composer.addDependency.ComposerPackage;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import net.servicestack.idea.common.Analytics;
import net.servicestack.idea.common.DialogErrorMessages;
import net.servicestack.idea.common.IDEAUtils;
import net.servicestack.idea.common.INativeTypesHandler;
import com.jetbrains.php.composer.actions.ComposerInstallAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AddPhpRefHandler {
    static void handleOk(Module module,
                         String addressUrl,
                         String fileName,
                         String selectedDirectory,
                         StringBuilder errorMessage) {
        File file = new File(selectedDirectory);
        INativeTypesHandler nativeTypesHandler =
                new PhpNativeTypesHandler();

        String dtoPath = file.getAbsolutePath() + File.separator
                + IDEAUtils.getDtoFileName(fileName, nativeTypesHandler);
        List<String> codeLines = getDtoLines(addressUrl, nativeTypesHandler, errorMessage);

        if (codeLines == null) {
            return;
        }

        tryUpdateComposerJson(module, errorMessage);

        if (!IDEAUtils.writeDtoFile(codeLines, dtoPath, errorMessage)) {
            return;
        }

        Analytics.SubmitAnonymousAddReferenceUsage(nativeTypesHandler);
        IDEAUtils.refreshFile(module, dtoPath, true);
        VirtualFileManager.getInstance().syncRefresh();
    }

    private static void tryUpdateComposerJson(Module module, StringBuilder errorMessage) {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        for (VirtualFile root : roots) {
            VirtualFile composerJsonFile = root.findChild("composer.json");

            if (composerJsonFile != null) {
                try {
                    Path path = Paths.get(composerJsonFile.getPath());
                    String content = new String(Files.readAllBytes(path));

                    // Parse JSON content to JSONObject
                    JSONObject composerJson = (JSONObject) JSONValue.parseWithException(content);

                    // Check if servicestack package is already added
                    if (composerJson.containsKey("require")) {
                        JSONObject requireSection = (JSONObject) composerJson.get("require");
                        if (requireSection.containsKey("servicestack/client")) {
                            return; // Exit early if servicestack already exists
                        }
                    }

                    // Add servicestack/client dependency via Composer Jetbrains APIs
                    // This is very inconsistent and doesn't always work
                    // Editing the `composer.json` manually causes errors from `composer`.
                    // Leaving this out until I can figure out a better way to do this.
                    installPackage(module);

                } catch (IOException | ParseException e) {
                    errorMessage.append(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void installPackage(Module module) {
        Project project = module.getProject();
        ComposerDataService composerDataService = project.getService(ComposerDataService.class);
        VirtualFile configFile = composerDataService.getConfigFile();
        if(configFile == null) {
            return;
        }
        ComposerPackageManager composerPackageManager = new ComposerPackageManager(project);
        ComposerPackageManager.DependentPackage dependentPackage = ComposerPackageManager.DependentPackage.SERVICESTACK_CLIENT;
        ComposerPackage composerPackage = composerPackageManager.findPackage(dependentPackage);
        if (composerPackage == null) {
            composerPackageManager.installPackage(dependentPackage, configFile);
        }
    }



    private static List<String> getDtoLines(String addressUrl, INativeTypesHandler nativeTypesHandler,
                                            StringBuilder errorMessage) {
        List<String> codeLines;
        try {
            codeLines = nativeTypesHandler.getUpdatedCode(addressUrl, null);
            // append the first two lines and check if it equals the native types handler comment start stripped of whitespace
            String firstLine = codeLines.get(0).replaceAll("\\s+", "");
            String secondLine = codeLines.get(1).replaceAll("\\s+", "");
            String commentStart = nativeTypesHandler.getOptionsCommentStart().replaceAll("\\s+", "");
            if (!(firstLine + secondLine).equals(commentStart)) {
                //Invalid endpoint
                errorMessage.append("The address url is not a valid ServiceStack endpoint.");
                return null;
            }
        } catch (URISyntaxException | MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
            DialogErrorMessages.appendInvalidEndpoint(errorMessage, addressUrl, e);
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            DialogErrorMessages.appendReadResponseError(errorMessage, addressUrl, e);
            return null;
        }
        return codeLines;
    }
}
