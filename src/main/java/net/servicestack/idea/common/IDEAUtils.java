package net.servicestack.idea.common;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Created by Layoric on 10/12/2015.
 */
public class IDEAUtils {

    // `module.getModuleFile()` now an internal API that cannot be used.
    // Removing to pass review.
//    public static void refreshBuildFile(Module module) {
//        VirtualFileManager.getInstance().syncRefresh();
//        if(module.getModuleFile() == null) { return; }
//
//        refreshFile(module,module.getModuleFile().getParent().getUrl() + "/build.gradle",
//                false);
//    }

    public static void refreshProject(Module module) {
        Project project = module.getProject();
        VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(
                Objects.requireNonNull(project.getBasePath())
        );
        if (virtualFile != null) {
            virtualFile.refresh(true,true);
        }
    }

    public static void refreshFile(Module module, String filePath, boolean openFile) {
        VirtualFileManager.getInstance().syncRefresh();
        File file = new File(filePath);
        VirtualFile fileByUrl = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

        if (fileByUrl == null) { return; }

        FileEditorManager.getInstance(module.getProject()).openFile(fileByUrl, false);
        Editor currentEditor = FileEditorManager.getInstance(module.getProject()).getSelectedTextEditor();
        if(currentEditor == null) { return; }
        Document document = currentEditor.getDocument();
        if (!openFile) closeFile(module,filePath);

        FileDocumentManager.getInstance().reloadFromDisk(document);
        VirtualFileManager.getInstance().syncRefresh();
    }

    public static boolean writeDtoFile(List<String> codeLines, String path, StringBuilder errorMessage) {
        BufferedWriter writer = null;
        boolean result = true;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(path)), StandardCharsets.UTF_8));
            for (String item : codeLines) {
                writer.write(item);
                writer.newLine();
            }
        } catch (IOException ex) {
            result = false;
            errorMessage.append("Error writing DTOs to file - ").append(ex.getMessage());
        } finally {
            try {
                if(writer != null)
                    writer.close();
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public static void closeFile(Module module, String filePath) {
        File file = new File(filePath);
        VirtualFile fileByUrl = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (fileByUrl == null) {
            return;
        }
        FileEditorManager.getInstance(module.getProject()).closeFile(fileByUrl);
    }

    public static String getDtoFileName(String name, INativeTypesHandler nativeTypesHandler) {
        String conventionName = name.endsWith("dtos") ? "" : ".dtos";
        int p = name.lastIndexOf(".");
        if (p == -1 || !name.substring(p).equals(nativeTypesHandler.getFileExtension())) {
            /* file has no extension */
            return name + conventionName + nativeTypesHandler.getFileExtension();
        } else {
            /* file has extension e */
            return name;
        }
    }

    public static String getInitialFileName(String path, INativeTypesHandler defaultTsNativeTypesHandler) {
        String initName = "dtos";
        File possibleFileName = new File(path + "/" + initName +
                defaultTsNativeTypesHandler.getFileExtension());
        if(!possibleFileName.exists())
            return possibleFileName.getName();
        int count = 1;
        while(true) {
            possibleFileName = new File(path + "/" + initName + count +
                    defaultTsNativeTypesHandler.getFileExtension());
            if(possibleFileName.exists()) {
                count++;
            } else {
                break;
            }
        }
        return possibleFileName.getName();
    }

    public static String getDtoNameWithoutExtension(String name, INativeTypesHandler nativeTypesHandler) {
        int p = name.lastIndexOf(".");
        if (p == -1 || !name.substring(p).equals(nativeTypesHandler.getFileExtension())) {
            /* file has no extension */
            return name;
        } else {
            /* file has extension e */
            return name.substring(0, p);
        }
    }

    public static ImageIcon createImageIcon(String path, String description) {
        URL imgURL = IDEAUtils.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}
