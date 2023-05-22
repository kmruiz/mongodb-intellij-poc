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

    public MQLQueryOrNotPerceived parse(@NotNull PsiMethodCallExpression methodCall) {
        var methodRefCall = methodCall.getMethodExpression().getCanonicalText();
        var queryDescription = new LinkedHashSet<MQLQuery.MQLQueryField>();

        if (
                methodRefCall.endsWith("find") ||
                        methodRefCall.endsWith("findOne")
        ) {
            var args = methodCall.getArgumentList();
            var argsTypes = args.getExpressionTypes();

            if (args.getExpressionCount() == 0) {
                return MQLQueryOrNotPerceived.notPerceived();
            }

            if (argsTypes[0].equalsToText("org.bson.Document") || argsTypes[0].equalsToText("org.bson.conversions.Bson")) {
                resolveBsonDocumentChain(args.getExpressions()[0], queryDescription);
            } else if (argsTypes.length > 1) { // session is the first parameter, so the query is the second
                resolveBsonDocumentChain(args.getExpressions()[1], queryDescription);
            }
        }

        return MQLQueryOrNotPerceived.perceived("test", "test", new MQLQuery(methodCall, queryDescription));
    }

    public MQLQueryOrNotPerceived parse(@NotNull PsiMethod method) {
        var returnType = method.getReturnType();
        if (returnType == null || !returnType.equalsToText("org.bson.Document")) {
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
            var method = methodCall.resolveMethod();
            var methodClass = method.getContainingClass();
            var methodArgTypes = methodCall.getTypeArguments();
            var methodArgs = methodCall.getArgumentList().getExpressions();
            var leftExpr = methodCall.getMethodExpression();

            if (methodClass.getQualifiedName().equals("com.mongodb.client.model.Filters")) {
                switch (method.getName()) {
                    case "eq":
                    case "ne":
                    case "gt":
                    case "lt":
                    case "gte":
                    case "lte":
                    case "in":
                    case "nin":
                    case "exists":
                    case "type":
                    case "mod":
                    case "regex":
                    case "all":
                    case "elemMatch":
                    case "size":
                    case "bitsAllClear":
                    case "bitsAllSet":
                    case "bitsAnyClear":
                    case "bitsAnySet":
                    case "geoWithin":
                    case "geoWithinBox":
                    case "geoWithinPolygon":
                    case "geoWithinCenter":
                    case "geoWithinCenterSphere":
                    case "geoIntersects":
                    case "near":
                    case "nearSphere":
                        queryDescription.add(resolveMQLQueryField(methodArgs[0]));
                        break;
                    case "and":
                    case "or":
                    case "nor":
                        if (method.isVarArgs()) {
                            for (var arg : methodArgs) {
                                resolveBsonDocumentChain(arg, queryDescription);
                            }
                        }
                        break;
                    default:
                }
            }

            if (leftExpr.getType().equalsToText("org.bson.Document") || leftExpr.getType().equalsToText("org.bson.conversions.Bson")) {
                resolveBsonDocumentChain(leftExpr.getQualifierExpression(), queryDescription);
            } else if (leftExpr.getType().equalsToText("org.bson.Document") || leftExpr.getType().equalsToText("org.bson.conversions.Bson")) {
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
