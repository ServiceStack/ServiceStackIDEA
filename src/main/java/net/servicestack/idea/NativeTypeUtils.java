package net.servicestack.idea;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.util.PlatformUtils;
import net.servicestack.idea.common.INativeTypesHandler;
import net.servicestack.idea.common.TypeScriptConcreteNativeTypesHandler;
import net.servicestack.idea.common.TypeScriptNativeTypesHandler;

public class NativeTypeUtils {

    public static INativeTypesHandler getNativeTypesHandler(String fileName) {
        INativeTypesHandler result = null;
        if (fileName.endsWith(".kt")) result = new KotlinNativeTypesHandler();
        if (fileName.endsWith(".java")) result = new JavaNativeTypesHandler();
        if (fileName.endsWith("dtos.dart")) result = new DartNativeTypesHandler();
        if (fileName.endsWith("dtos.ts")) result = new TypeScriptConcreteNativeTypesHandler();
        if (fileName.endsWith("dtos.d.ts")) result = new TypeScriptNativeTypesHandler();
        if (fileName.endsWith("dtos.py")) result = new PythonNativeTypesHandler();
        return result;
    }
}
