package cat.kmruiz.mongodb.infrastructure;

import cat.kmruiz.mongodb.services.mql.ast.Node;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;
import java.util.Objects;

public final class PsiMongoDBTreeUtils {
    private PsiMongoDBTreeUtils() {}

    public static PsiMethodCallExpression getMongoDBQueryExpression(PsiElement parent) {
        var currentMethodExpression = PsiTreeUtil.getParentOfType(parent, PsiMethodCallExpression.class);

        if (currentMethodExpression == null) {
            return null;
        }

        var resolvedMethod = currentMethodExpression.resolveMethod();

        if (resolvedMethod == null) {
            return null;
        }

        var owningClass = resolvedMethod.getContainingClass();
        if (owningClass.getQualifiedName().equals("com.mongodb.client.MongoCollection")) {
            return null;
        }

        return Objects.requireNonNullElse(getMongoDBQueryExpression(currentMethodExpression.getParent()), currentMethodExpression);
    }

    public static PsiMethodCallExpression asMongoDBExpression(PsiElement parent) {
        if (parent instanceof PsiMethodCallExpression currentMethodExpression) {
            var resolvedMethod = currentMethodExpression.resolveMethod();

            if (resolvedMethod == null) {
                return null;
            }

            var owningClass = resolvedMethod.getContainingClass();
            if (owningClass.getQualifiedName().equals("com.mongodb.client.MongoCollection")) {
                return null;
            }

            return currentMethodExpression;
        }

        return null;
    }

    public static Node findNodeParentOf(List<Node> listOfParents, PsiElement reference) {
        if (listOfParents.isEmpty() || reference == null) {
            return null;
        }

        var parent = reference.getParent();
        for (var el : listOfParents) {
            if (el.origin() == parent) {
                return el;
            }
        }

        return findNodeParentOf(listOfParents, parent);
    }
}
