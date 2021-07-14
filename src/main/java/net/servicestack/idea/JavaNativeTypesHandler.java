package net.servicestack.idea;

/**
 * Created by Layoric on 4/12/2015.
 */
public class JavaNativeTypesHandler extends BaseNativeTypesHandler {
    @Override
    public String getFileExtension() {
        return ".java";
    }

    @Override
    public String getRelativeTypesUrl() {
        return "types/java";
    }

    @Override
    public NativeTypesLanguage getTypesLanguage() {
        return NativeTypesLanguage.Java;
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
