package cat.kmruiz.mongodb.lang.java.quickfix;

import cat.kmruiz.mongodb.services.mql.MQLQuery;
import cat.kmruiz.mongodb.ui.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class RunQueryOnSecondaryNode implements LocalQuickFix {
    public RunQueryOnSecondaryNode() {
    }

    @NotNull
    @Override
    public String getName() {
        return QuickFixBundle.message("quickfix.RunQueryOnSecondaryNode.name");
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        var methodCallExpression = (PsiMethodCallExpression) descriptor.getPsiElement();
        var qualifier = methodCallExpression.getMethodExpression().getQualifierExpression();
        var factory = JavaPsiFacade.getInstance(project).getElementFactory();

        var callWithReadPref = (PsiMethodCallExpression) factory.createExpressionFromText("c.withReadPreference(ReadPreference.secondaryPreferred())", null);

        callWithReadPref.getMethodExpression().getQualifierExpression().replace(qualifier);
        methodCallExpression.getMethodExpression().getQualifierExpression().replace(callWithReadPref);
    }
}
