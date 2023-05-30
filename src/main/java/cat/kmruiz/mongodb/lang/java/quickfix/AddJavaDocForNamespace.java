package cat.kmruiz.mongodb.lang.java.quickfix;

import cat.kmruiz.mongodb.ui.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaDocumentedElement;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class AddJavaDocForNamespace implements LocalQuickFix {
    private final PsiJavaDocumentedElement collectionDeclaration;

    public AddJavaDocForNamespace(PsiJavaDocumentedElement collectionDeclaration) {
        this.collectionDeclaration = collectionDeclaration;
    }

    @NotNull
    @Override
    public String getName() {
        return QuickFixBundle.message("quickfix.AddJavaDocForNamespace.name");
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        var factory = JavaPsiFacade.getInstance(project).getElementFactory();
        var docComment = factory.createDocCommentFromText("""
                /**
                  * @namespace database.collection
                  **/""");
        collectionDeclaration.getParent().addBefore(docComment, collectionDeclaration);

    }
}
