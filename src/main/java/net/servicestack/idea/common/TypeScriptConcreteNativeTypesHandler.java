package net.servicestack.idea.common;

/**
 * Created by Layoric on 13/05/2016.
 */
public class TypeScriptConcreteNativeTypesHandler extends BaseNativeTypesHandler {
    @Override
    public String getFileExtension() {
        return ".ts";
    }

    @Override
    public String getRelativeTypesUrl() {
        return "types/typescript";
    }

    @Override
    public NativeTypesLanguage getTypesLanguage() {
        return NativeTypesLanguage.TypeScriptConcrete;
    }

    @Override
    public String getOptionsCommentStart() {
        return "*/";
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
