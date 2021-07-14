package net.servicestack.idea;

/**
 * Created by Layoric on 4/12/2015.
 */
public class KotlinNativeTypesHandler extends BaseNativeTypesHandler {
    @Override
    public String getFileExtension() {
        return ".kt";
    }

    @Override
    public String getRelativeTypesUrl() {
        return "types/kotlin";
    }

    @Override
    public NativeTypesLanguage getTypesLanguage() {
        return NativeTypesLanguage.Kotlin;
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
