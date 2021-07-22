package net.servicestack.idea.common;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import net.servicestack.idea.NativeTypeUtils;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by Layoric on 10/12/2015.
 */
public class IDEAUtils {

    public static void refreshBuildFile(Module module) {
        VirtualFileManager.getInstance().syncRefresh();
        if(module.getModuleFile() == null) { return; }

        VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(module.getModuleFile().getParent().getUrl() + "/build.gradle");

        if(fileByUrl == null) { return; }

        FileEditorManager.getInstance(module.getProject()).openFile(fileByUrl, false);
        Editor currentEditor = FileEditorManager.getInstance(module.getProject()).getSelectedTextEditor();
        if(currentEditor == null) { return; }
        Document document = currentEditor.getDocument();

        FileDocumentManager.getInstance().reloadFromDisk(document);
        VirtualFileManager.getInstance().syncRefresh();
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
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
            for (String item : codeLines) {
                writer.write(item);
                writer.newLine();
            }
        } catch (IOException ex) {
            result = false;
            errorMessage.append("Error writing DTOs to file - ").append(ex.getMessage());
        } finally {
            try {
                assert writer != null;
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
        if (!name.endsWith(nativeTypesHandler.getFileExtension())) {
            /* file has no extension */
            return name + nativeTypesHandler.getFileExtension();
        } else {
            /* file has extension */
            return name;
        }
    }

    public static String getDtoFileName(String name) {
        INativeTypesHandler nativeTypesHandler = NativeTypeUtils.getNativeTypesHandler(name);
        int p = name.lastIndexOf(".");
        if (p == -1 || !name.substring(p).equals(nativeTypesHandler.getFileExtension())) {
            /* file has no extension */
            return name + nativeTypesHandler.getFileExtension();
        } else {
            /* file has extension e */
            return name;
        }
    }

    public static String getInitialFileName(String path, INativeTypesHandler defaultTsNativeTypesHandler) {
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

    public static String getDtoNameWithoutExtension(String name) {
        INativeTypesHandler nativeTypesHandler = NativeTypeUtils.getNativeTypesHandler(name);
        int p = name.lastIndexOf(".");
        if (p == -1 || !name.substring(p).equals(nativeTypesHandler.getFileExtension())) {
            /* file has no extension */
            return name;
        } else {
            /* file has extension e */
            return name.substring(0, p);
        }
    }

    public static ImageIcon createImageIcon(String path, String description, Class ownerClass) {
        URL imgURL = ownerClass.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
}
