package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.lang.java.mql.JavaMQLParser;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MQLIndexChecker;
import cat.kmruiz.mongodb.services.mql.MQLTypeChecker;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

public class IndexCheckingInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        var indexChecker = holder.getProject().getService(MQLIndexChecker.class);
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
                        indexChecker.checkIndexes(query, holder);
                    }
                }
            }
        };
    }
}
