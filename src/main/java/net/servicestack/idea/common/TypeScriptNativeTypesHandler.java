package net.servicestack.idea.common;

/**
 * Created by Layoric on 29/05/2016.
 */
public class TypeScriptNativeTypesHandler extends BaseNativeTypesHandler {
    @Override
    public String getFileExtension() {
        return ".d.ts";
    }

    @Override
    public String getRelativeTypesUrl() {
        return "types/typescript.d";
    }

    @Override
    public NativeTypesLanguage getTypesLanguage() {
        return NativeTypesLanguage.TypeScript;
    }

    @Override
    public String getOptionsCommentStart() {
        return "/* Options:";
    }

    @Override
    public String getOptionsCommentEnd() {
        return "*/";
    }

    @Override
    public String getOptionsIgnoreFlag() {
        return "//";
    }
}
