package net.servicestack.idea;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.requirement.PyRequirementRelation;
import net.servicestack.idea.common.Analytics;
import net.servicestack.idea.common.DialogErrorMessages;
import net.servicestack.idea.common.IDEAUtils;
import net.servicestack.idea.common.INativeTypesHandler;

import static com.jetbrains.python.packaging.PyRequirementsKt.pyRequirement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

public class AddPythonRefHandler {
    static void handleOk(Module module,
                         String addressUrl,
                         String fileName,
                         String selectedDirectory,
                         StringBuilder errorMessage) {
        File file = new File(selectedDirectory);
        INativeTypesHandler nativeTypesHandler =
                new PythonNativeTypesHandler();

        String dtoPath = file.getAbsolutePath() + File.separator
                + IDEAUtils.getDtoFileName(fileName, nativeTypesHandler);
        List<String> codeLines = getDtoLines(addressUrl, nativeTypesHandler, errorMessage);

        if (codeLines == null) {
            return;
        }

        tryUpdateRequirementsTxt(module, errorMessage);

        if (!IDEAUtils.writeDtoFile(codeLines, dtoPath, errorMessage)) {
            return;
        }

        Analytics.SubmitAnonymousAddReferenceUsage(nativeTypesHandler);
        IDEAUtils.refreshFile(module, dtoPath, true);
        VirtualFileManager.getInstance().syncRefresh();
    }

    private static void tryUpdateRequirementsTxt(Module module, StringBuilder errorMessage) {
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(module.getProject());
        Sdk sdk = projectRootManager.getProjectSdk();
        if (sdk == null)
            return;
        PyPackageManager pyPackageManager = PyPackageManager.getInstance(sdk);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Thread.sleep(100);
                pyPackageManager.refreshAndGetPackages(false);
                pyPackageManager.install(Collections.singletonList(
                        pyRequirement("servicestack", PyRequirementRelation.GTE, "0.0.5")),
                        Collections.emptyList());
                pyPackageManager.refreshAndGetPackages(true);
                IDEAUtils.refreshProject(module);
                ApplicationManager.getApplication().invokeLater(() ->
                        VirtualFileManager.getInstance().refreshWithoutFileWatcher(false));
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.append(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static List<String> getDtoLines(String addressUrl, INativeTypesHandler nativeTypesHandler,
                                            StringBuilder errorMessage) {
        List<String> codeLines;
        try {
            codeLines = nativeTypesHandler.getUpdatedCode(addressUrl, null);
            if (!codeLines.get(0).startsWith("\"\"\" Options:")) {
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
