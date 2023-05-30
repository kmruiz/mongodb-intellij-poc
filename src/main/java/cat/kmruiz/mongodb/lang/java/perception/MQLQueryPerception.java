package cat.kmruiz.mongodb.lang.java.perception;

import cat.kmruiz.mongodb.services.mql.MQLQuery;
import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MQLQueryPerception {
    public enum PerceptionFailure {
        NO_QUERY,
        NO_NAMESPACE
    }

    public record MQLQueryOrNotPerceived(String database, String collection, MQLQuery query, PerceptionFailure failure, PsiJavaDocumentedElement collectionDeclaration) {
        public boolean hasBeenPerceived() {
            return query != null;
        }

        public static MQLQueryOrNotPerceived notPerceived(PerceptionFailure failure, PsiJavaDocumentedElement collectionDeclaration) {
            return new MQLQueryOrNotPerceived(null, null,null, failure, collectionDeclaration);
        }
        public static MQLQueryOrNotPerceived perceived(String database, String collection, MQLQuery query, PsiJavaDocumentedElement collectionDeclaration) {
            return new MQLQueryOrNotPerceived(database, collection,query, null, collectionDeclaration);
        }
    }

    public MQLQueryOrNotPerceived parse(@NotNull PsiMethodCallExpression methodCall) {
        var methodRefCall = methodCall.getMethodExpression().getCanonicalText();
        var queryDescription = new LinkedHashSet<MQLQuery.MQLQueryField>();

        var declarationOfCollection = findReferenceToCollection(methodCall);
        if (declarationOfCollection == null) {
            return MQLQueryOrNotPerceived.perceived("test", "test", new MQLQuery(methodCall, queryDescription), null);
        }

        var mdbNamespace = inferNamespace(declarationOfCollection);

        if (
                methodRefCall.endsWith("find") ||
                        methodRefCall.endsWith("findOne")
        ) {
            var args = methodCall.getArgumentList();
            var argsTypes = args.getExpressionTypes();

            if (args.getExpressionCount() == 0) {
                return MQLQueryOrNotPerceived.notPerceived(PerceptionFailure.NO_QUERY, (PsiJavaDocumentedElement) declarationOfCollection);
            }

            if (argsTypes[0].equalsToText("org.bson.Document") || argsTypes[0].equalsToText("org.bson.conversions.Bson")) {
                resolveBsonDocumentChain(args.getExpressions()[0], queryDescription);
            } else if (argsTypes.length > 1) { // session is the first parameter, so the query is the second
                resolveBsonDocumentChain(args.getExpressions()[1], queryDescription);
            }
        }

        if (mdbNamespace.isKnown()) {
            return MQLQueryOrNotPerceived.perceived(mdbNamespace.database(), mdbNamespace.collection(), new MQLQuery(methodCall, queryDescription), (PsiJavaDocumentedElement) declarationOfCollection);
        } else {
            return MQLQueryOrNotPerceived.notPerceived(PerceptionFailure.NO_NAMESPACE, (PsiJavaDocumentedElement) declarationOfCollection);
        }
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
            if (method == null) {
                return;
            }

            var methodClass = method.getContainingClass();
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
            var resolvedValue = inferConstantStringValue(expression);
            if (resolvedValue == null) {
                return MQLQuery.MQLQueryField.newWildcard();
            } else {
                return MQLQuery.MQLQueryField.named(resolvedValue);
            }
        }
    }

    private PsiElement findReferenceToCollection(PsiElement el) {
        if (el instanceof PsiMethodCallExpression methodCall) {
            return findReferenceToCollection(methodCall.getMethodExpression());
        } else if (el instanceof PsiReferenceExpression refExpr) {
            var qualifierIfAny = refExpr.getQualifierExpression();
            if (qualifierIfAny != null) {
                if (!(qualifierIfAny instanceof PsiThisExpression)) {
                    return findReferenceToCollection(qualifierIfAny);
                }
            }

            var reference = refExpr.resolve();
            if (reference instanceof PsiLocalVariable localVar) {
                if (localVar.getType().getCanonicalText().startsWith("com.mongodb.client.MongoCollection")) {
                    return localVar;
                }
            } else if (reference instanceof PsiField classField) {
                if (classField.getType().getCanonicalText().startsWith("com.mongodb.client.MongoCollection")) {
                    return classField;
                }
            }
        } else if (el instanceof PsiExpression expr) {
            return findReferenceToCollection(expr.getFirstChild());
        }

        return null;
    }

    private MongoDBNamespace inferNamespace(PsiElement el) {
        if (el instanceof PsiLocalVariable localVar) {
            return inferNamespaceFromInitialisation(localVar.getInitializer());
        } else if (el instanceof PsiField classField) {
            var docComment = classField.getDocComment();
            if (docComment != null) {
                var namespaceTag = docComment.findTagByName("namespace");
                if (namespaceTag != null) {
                    var namespaceValue = namespaceTag.getValueElement();
                    if (namespaceValue != null) {
                        return MongoDBNamespace.fromQualifiedNamespace(namespaceValue.getText());
                    }
                }
            }

            if (classField.hasInitializer()) {
                return inferNamespaceFromInitialisation(classField.getInitializer());
            } else {
                return Arrays.stream(classField.getContainingClass().getConstructors())
                        .map(constructor -> this.inferFromConstructor(classField.getName(), constructor))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .map(Optional::get)
                        .orElse(MongoDBNamespace.defaultTest());
            }
        }

        return MongoDBNamespace.defaultTest();
    }

    private Optional<MongoDBNamespace> inferFromConstructor(String fieldName, PsiMethod constructor) {
        var body = constructor.getBody();
        for (var stmt : body.getStatements()) {
            if (stmt instanceof PsiExpressionStatement exprStmt) {
                var expr = exprStmt.getExpression();
                if (expr instanceof PsiAssignmentExpression assignmentExpr) {
                    if (assignmentExpr.getLExpression().getText().endsWith(fieldName)) {
                        return Optional.of(inferNamespaceFromInitialisation(assignmentExpr.getRExpression()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private MongoDBNamespace inferNamespaceFromInitialisation(PsiExpression el) {
        String database = "", collection = "";

        if (el instanceof PsiMethodCallExpression methodCall) {
            if (methodCall.getMethodExpression().getText().endsWith("getCollection")) {
                var collectionNameEl = methodCall.getArgumentList().getExpressions()[0];
                collection = inferConstantStringValue(collectionNameEl);
                database = inferDatabaseName(methodCall.getMethodExpression().getQualifierExpression());
            }
        }

        return new MongoDBNamespace(database, collection);
    }

    private String inferDatabaseName(PsiElement el) {
        if (el instanceof PsiMethodCallExpression methodCall) {
            if (methodCall.getMethodExpression().getText().endsWith("getDatabase")) {
                var collectionNameEl = methodCall.getArgumentList().getExpressions()[0];
                return inferConstantStringValue(collectionNameEl);
            } else {
                return inferDatabaseName(methodCall.getMethodExpression().getQualifierExpression());
            }
        } else if (el instanceof PsiLocalVariable localVar) {
            return inferDatabaseName(localVar.getInitializer());
        } else if (el instanceof PsiField field) {
            return inferDatabaseName(field.getInitializer());
        } else if (el instanceof PsiReferenceExpression refExpr) {
            return inferDatabaseName(refExpr.resolve());
        }

        return null;
    }

    private String inferConstantStringValue(PsiElement expression) {
        if (expression instanceof PsiReferenceExpression refExpr) {
            var varRef = refExpr.resolve();
            return inferConstantStringValue(varRef);
        } else if (expression instanceof PsiLocalVariable localVar) {
            return inferConstantStringValue(localVar.getInitializer());
        } else if (expression instanceof PsiLiteralValue literalVal) {
            return literalVal.getValue().toString();
        } else if (expression instanceof PsiField field) {
            var computed = field.computeConstantValue();
            if (computed != null) {
                return computed.toString();
            }
        }

        return null;
    }

}
