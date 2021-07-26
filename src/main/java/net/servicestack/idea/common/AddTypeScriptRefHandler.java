package net.servicestack.idea.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

public class AddTypeScriptRefHandler {

    static void handleOk(Module module,
                         String addressUrl,
                         String fileName,
                         String selectedDirectory,
                         Boolean definitionsOnly,
                         StringBuilder errorMessage) {
        File file = new File(selectedDirectory);
        INativeTypesHandler nativeTypesHandler =
                definitionsOnly ?
                        new TypeScriptNativeTypesHandler() :
                        new TypeScriptConcreteNativeTypesHandler();

        String dtoPath = file.getAbsolutePath() + File.separator
                + IDEAUtils.getDtoFileName(fileName,nativeTypesHandler);
        List<String> codeLines = getDtoLines(addressUrl,nativeTypesHandler,errorMessage);

        if(codeLines == null) {
            return;
        }

        if (!IDEAUtils.writeDtoFile(codeLines, dtoPath, errorMessage)) {
            return;
        }

        Analytics.SubmitAnonymousAddReferenceUsage(nativeTypesHandler);
        IDEAUtils.refreshFile(module,dtoPath, true);
        VirtualFileManager.getInstance().syncRefresh();
    }

    private static List<String> getDtoLines(String addressUrl, INativeTypesHandler nativeTypesHandler,
                                            StringBuilder errorMessage) {
        List<String> codeLines;
        try {
            codeLines = nativeTypesHandler.getUpdatedCode(addressUrl, null);
            if (!codeLines.get(0).startsWith("/* Options:")) {
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
