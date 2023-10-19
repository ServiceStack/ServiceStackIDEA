package net.servicestack.idea;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import net.servicestack.idea.common.Analytics;
import net.servicestack.idea.common.INativeTypesHandler;
import net.servicestack.idea.common.NativeTypesLanguage;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by Layoric on 9/04/2015.
 * Helper methods to handle Update Reference intention.
 */
public class UpdateServiceStackUtils {

    public static void updateServiceStackReference(PsiFile psiFile) {
        String code = psiFile.getText();
        Scanner scanner = new Scanner(code);
        List<String> linesOfCode = new ArrayList<>();
        INativeTypesHandler nativeTypesHandler = NativeTypeUtils.getNativeTypesHandler(psiFile.getName());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            linesOfCode.add(line);
            if (line.startsWith(nativeTypesHandler.getOptionsCommentEnd()) &&
            !line.endsWith(nativeTypesHandler.getOptionsCommentStart()))
                break;
        }
        scanner.close();

        int startParamsIndex = 0;
        String baseUrl = null;
        for (String item : linesOfCode) {
            startParamsIndex++;
            if (item.startsWith("BaseUrl:")) {
                baseUrl = item.split(":",2)[1].trim();
                break;
            }
        }
        if (baseUrl == null) {
            //noinspection UnresolvedPluginConfigReference
            Notification notification = new Notification("ServiceStackIDEA", "Error updating reference", "BaseUrl property not found.", NotificationType.ERROR);
            Notifications.Bus.notify(notification);
            return;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        URIBuilder builder;
        try {
            builder = new URIBuilder(baseUrl);
        } catch (URISyntaxException e) {
            //Log error to IDEA warning bubble/window.
            //noinspection UnresolvedPluginConfigReference
            Notification notification = new Notification("ServiceStackIDEA", "Error updating reference", "Invalid BaseUrl provided", NotificationType.ERROR);
            Notifications.Bus.notify(notification);
            return;
        }

        String existingPath = builder.getPath();
        if (existingPath == null || existingPath.equals("/")) {
            builder.setPath(combinePath("", nativeTypesHandler.getRelativeTypesUrl()));
        } else {
            builder.setPath(combinePath(existingPath, nativeTypesHandler.getRelativeTypesUrl()));
        }

        Map<String,String> options = new HashMap<>();
        for (int i = startParamsIndex; i < linesOfCode.size(); i++) {
            String configLine = linesOfCode.get(i);
            if(configLine.startsWith(nativeTypesHandler.getOptionsCommentEnd())) {
                break;
            }
            if (!configLine.startsWith(nativeTypesHandler.getOptionsIgnoreFlag()) && configLine.contains(":")) {
                String[] keyVal = configLine.split(":");
                options.put(keyVal[0], keyVal[1].trim());
            }
        }

        try {
            StringBuilder serverUrl = new StringBuilder(builder.build().toString());
            int count = 0;
            // Using URIBuilder with 'addParameter' URL encodes query values..
            // Append manually below to avoid issues https://github.com/ServiceStack/ServiceStack.Java/issues/6
            for (Map.Entry<String,String> option : options.entrySet()) {
                if(count == 0) {
                    serverUrl.append("?");
                } else {
                    serverUrl.append("&");
                }
                //remove spaces
                serverUrl.append(option.getKey()).append("=").append(option.getValue().trim().replaceAll("\\u0020", ""));
                count++;
            }
            StringBuilder javaCodeResponse = getJavaCodeResponse(serverUrl);

            String javaCode = javaCodeResponse.toString();
            // We need to check PHP differently since it is the only language that requires a namespace
            // declared at the top of the file.
            if (!javaCode.startsWith(nativeTypesHandler.getOptionsCommentStart()) && !isValidPhpResponse(javaCode)) {
                //noinspection UnresolvedPluginConfigReference
                Notification notification = new Notification("ServiceStackIDEA", "Error updating reference", "Invalid response from provided BaseUrl - " + baseUrl, NotificationType.ERROR);
                Notifications.Bus.notify(notification);
                return;
            }
            Document document = FileDocumentManager.getInstance().getDocument(psiFile.getVirtualFile());
            if (document != null) {
                document.setText(javaCodeResponse);
                Analytics.SubmitAnonymousUpdateReferenceUsage(nativeTypesHandler);
            } else {
                //Show error
                //noinspection UnresolvedPluginConfigReference
                Notification notification = new Notification("ServiceStackIDEA", "Error updating reference", "DTO file not found.", NotificationType.ERROR);
                Notifications.Bus.notify(notification);
            }
        } catch (Exception e) {
            //noinspection UnresolvedPluginConfigReference
            Notification notification = new Notification("ServiceStackIDEA", "Error updating reference", e.getMessage(), NotificationType.ERROR);
            Notifications.Bus.notify(notification);
            e.printStackTrace();
        }
    }


    private static boolean isValidPhpResponse(String phpCodeResponse) {
        // First split into lines, we only need to test against the first 5
        String[] phpCodeLines = phpCodeResponse.split("\n", 5);
        // check the size of the array
        if (phpCodeLines.length < 2) {
            return false;
        }

        // Check if the first line is the namespace
        if (!phpCodeLines[0].startsWith("<?php namespace")) {
            return false;
        }

        // Check if the second line is the options comment start
        return phpCodeLines[1].startsWith("/* Options:");
    }

    @NotNull
    private static StringBuilder getJavaCodeResponse(StringBuilder serverUrl) throws IOException {
        URL javaCodeUrl = new URL(serverUrl.toString());

        URLConnection javaCodeConnection = javaCodeUrl.openConnection();
        javaCodeConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
        BufferedReader javaCodeBufferReader = new BufferedReader(
                new InputStreamReader(
                        javaCodeConnection.getInputStream()));
        String javaCodeInput;
        StringBuilder javaCodeResponse = new StringBuilder();
        while ((javaCodeInput = javaCodeBufferReader.readLine()) != null) {
            javaCodeResponse.append(javaCodeInput);
            //All documents inside IntelliJ IDEA always use \n line separators.
            //http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview
            javaCodeResponse.append("\n");
        }
        return javaCodeResponse;
    }

    public static String combinePath(String path, String segment) {
        if (path == null || path.isEmpty()) {
            return "/" + segment;
        }

        if (path.charAt(path.length() - 1) == '/') {
            return path + segment;
        }

        return path + "/" + segment;
    }

    public static boolean containsOptionsHeader(PsiFile psiJavaFile) {
        Document dtoDocument = FileDocumentManager.getInstance().getDocument(psiJavaFile.getVirtualFile());
        if(dtoDocument == null) {
            return false;
        }
        //Only pull in the first 1000 chars max to look for header.
        int range = Math.min(dtoDocument.getTextLength(), 1000);
        String code = dtoDocument.getText(new TextRange(0, range));

        String[] codeLines = code.split("\n");
        for(String line : codeLines) {
            if(line.startsWith("BaseUrl:")) {
                return true;
            }
        }
        return false;
    }
}
