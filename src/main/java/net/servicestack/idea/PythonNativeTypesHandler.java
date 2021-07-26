package net.servicestack.idea;

import net.servicestack.idea.common.BaseNativeTypesHandler;
import net.servicestack.idea.common.NativeTypesLanguage;

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
        return "\"\"\" Options:";
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
