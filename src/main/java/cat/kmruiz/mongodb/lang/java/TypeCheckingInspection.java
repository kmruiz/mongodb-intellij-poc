package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.lang.java.mql.JavaMQLParser;
import cat.kmruiz.mongodb.lang.java.perception.MQLQueryPerception;
import cat.kmruiz.mongodb.lang.java.quickfix.AddJavaDocForNamespace;
import cat.kmruiz.mongodb.lang.java.quickfix.DeduceIndexQuickFix;
import cat.kmruiz.mongodb.lang.java.quickfix.RunQueryOnSecondaryNode;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MQLTypeChecker;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.ui.IndexBeautifier;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

public class TypeCheckingInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        var typeChecker = holder.getProject().getService(MQLTypeChecker.class);
        var parser = holder.getProject().getService(JavaMQLParser.class);

        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                var resolvedMethod = expression.resolveMethod();

                if (resolvedMethod == null) {
                    return;
                }

                var owningClass = resolvedMethod.getContainingClass();

                if (owningClass.getQualifiedName().equals("com.mongodb.client.MongoCollection")) {
                    var parsedQuery = parser.parse(expression);
                    if (parsedQuery instanceof QueryNode<PsiElement> query) {
                        typeChecker.typeCheck(query, holder);
                    }
                }
            }
        };
    }
}
