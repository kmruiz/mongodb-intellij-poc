package cat.kmruiz.mongodb.lang.java.perception;

import cat.kmruiz.mongodb.services.mql.MQLQuery;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class MQLQueryPerception {
    public record MQLQueryOrNotPerceived(String database, String collection, MQLQuery query) {
        public boolean hasBeenPerceived() {
            return query != null;
        }

        public static MQLQueryOrNotPerceived notPerceived() {
            return new MQLQueryOrNotPerceived(null, null,null);
        }
        public static MQLQueryOrNotPerceived perceived(String database, String collection, MQLQuery query) {
            return new MQLQueryOrNotPerceived(database, collection,query);
        }
    }

    public MQLQueryOrNotPerceived parse(@NotNull PsiMethod method) {
        if (!method.getReturnType().equalsToText("org.bson.Document")) {
            return MQLQueryOrNotPerceived.notPerceived();
        }

        var queryDescription = new LinkedHashSet<MQLQuery.MQLQueryField>();
        for (var child : method.getBody().getStatements()) {
            if (child instanceof PsiReturnStatement returnStmt) {
                resolveBsonDocumentChain(returnStmt.getReturnValue(), queryDescription);
            }
        }

        if (queryDescription.isEmpty()) {
            return MQLQueryOrNotPerceived.notPerceived();
        }

        return MQLQueryOrNotPerceived.perceived("test", "test", new MQLQuery(method, queryDescription));
    }

    private void resolveBsonDocumentChain(PsiExpression expr, LinkedHashSet<MQLQuery.MQLQueryField> queryDescription) {
        if (expr instanceof PsiNewExpression newDoc) {
            var constructorMethod = newDoc.resolveConstructor();
            var returnType = constructorMethod.getContainingClass();

            if (returnType.getQualifiedName().equals("org.bson.Document")) {
                // we are in "new Document(key, value)"
                var methodArgs = newDoc.getArgumentList().getExpressions();
                if (methodArgs.length == 2) {
                    queryDescription.add(resolveMQLQueryField(methodArgs[0]));
                }
            }
        } else if (expr instanceof PsiMethodCallExpression methodCall) {
            var methodArgs = methodCall.getArgumentList().getExpressions();
            var leftExpr = methodCall.getMethodExpression();

            if (leftExpr.getType().equalsToText("org.bson.Document")) {
                resolveBsonDocumentChain(leftExpr.getQualifierExpression(), queryDescription);
            }

            if (leftExpr.getCanonicalText().endsWith(".append")) {
                if (methodArgs.length == 2) {
                    queryDescription.add(resolveMQLQueryField(methodArgs[0]));
                }
            }
        }
    }

    private MQLQuery.MQLQueryField resolveMQLQueryField(PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression) {
            return MQLQuery.MQLQueryField.named(expression.getText().replaceAll("\"", ""));
        } else {
            return MQLQuery.MQLQueryField.newWildcard();
        }
    }
}
