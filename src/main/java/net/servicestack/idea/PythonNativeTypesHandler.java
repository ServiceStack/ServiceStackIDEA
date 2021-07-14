package net.servicestack.idea;

public class PythonNativeTypesHandler extends BaseNativeTypesHandler {
    @Override
    public String getFileExtension() {
        return ".py";
    }

    @Override
    public String getRelativeTypesUrl() {
        return "types/python";
    }

    @Override
    public NativeTypesLanguage getTypesLanguage() {
        return NativeTypesLanguage.Python;
    }

    @Override
    public String getOptionsCommentStart() {
        return "\"\"\"";
    }

    @Override
    public String getOptionsCommentEnd() {
        return "\"\"\"";
    }

    @Override
    public String getOptionsIgnoreFlag() {
        return "#";
    }
}
