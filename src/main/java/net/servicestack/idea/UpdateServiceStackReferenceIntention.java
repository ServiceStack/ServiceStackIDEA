package net.servicestack.idea;

import com.intellij.codeInsight.intention.impl.QuickEditAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import net.servicestack.idea.common.INativeTypesHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class UpdateServiceStackReferenceIntention extends QuickEditAction implements Iconable {

    @NotNull
    @Override
    public String getText() {
        return "Update ServiceStack reference";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "UpdateServiceStackReferenceIntention";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        try {
            if(psiFile == null) {
                return false;
            }
            INativeTypesHandler nativeTypesHandler = NativeTypeUtils.getNativeTypesHandler(psiFile.getName());
            if(nativeTypesHandler == null) {
                return false;
            }

            if(UpdateServiceStackUtils.containsOptionsHeader(psiFile)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, final PsiFile psiFile) throws IncorrectOperationException {
        // First check if on write thread
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    UpdateServiceStackUtils.updateServiceStackReference(psiFile);
                }
            });
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public Icon getIcon(@IconFlags int i) {
        return IconLoader.getIcon("/servicestack.svg",UpdateServiceStackReferenceIntention.class);
    }
}
