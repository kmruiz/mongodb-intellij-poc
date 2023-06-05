package cat.kmruiz.mongodb.lang.java.perception;

import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.MQLQuery;
import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import cat.kmruiz.mongodb.services.mql.reporting.QueryWarning;
import cat.kmruiz.mongodb.services.schema.CollectionSchema;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MQLQueryPerception {
    public enum PerceptionFailure {
        NO_QUERY,
        NO_NAMESPACE
    }

    private final MongoDBFacade mongoDBFacade;

    public MQLQueryPerception(MongoDBFacade mongoDBFacade) {
        this.mongoDBFacade = mongoDBFacade;
    }

    public record MQLQueryOrNotPerceived(String database, String collection, MQLQuery<PsiElement> query, PerceptionFailure failure, PsiJavaDocumentedElement collectionDeclaration) {
        public boolean hasBeenPerceived() {
            return query != null;
        }

        public static MQLQueryOrNotPerceived notPerceived(PerceptionFailure failure, PsiJavaDocumentedElement collectionDeclaration) {
            return new MQLQueryOrNotPerceived(null, null,null, failure, collectionDeclaration);
        }
        public static MQLQueryOrNotPerceived perceived(String database, String collection, MQLQuery<PsiElement> query, PsiJavaDocumentedElement collectionDeclaration) {
            return new MQLQueryOrNotPerceived(database, collection, query, null, collectionDeclaration);
        }
    }

    public MQLQueryOrNotPerceived parse(@NotNull PsiMethodCallExpression methodCall) {
        var methodRefCall = methodCall.getMethodExpression().getCanonicalText();
        var queryDescription = new LinkedHashSet<MQLQuery.Predicate<PsiElement>>();

        var declarationOfCollection = findReferenceToCollection(methodCall);
        if (declarationOfCollection == null) {
            return MQLQueryOrNotPerceived.notPerceived(PerceptionFailure.NO_NAMESPACE, null);
        }

        var mdbNamespace = inferNamespace(declarationOfCollection);
        var schemaOfCollection = mongoDBFacade.schemaOf(mdbNamespace.database(), mdbNamespace.collection());

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
                resolveBsonDocumentChain(args.getExpressions()[0], queryDescription, schemaOfCollection);
            } else if (argsTypes.length > 1) { // session is the first parameter, so the query is the second
                resolveBsonDocumentChain(args.getExpressions()[1], queryDescription, schemaOfCollection);
            }
        }

        if (mdbNamespace.isKnown()) {
            return MQLQueryOrNotPerceived.perceived(mdbNamespace.database(), mdbNamespace.collection(), new MQLQuery<>(methodCall, queryDescription), (PsiJavaDocumentedElement) declarationOfCollection);
        } else {
            return MQLQueryOrNotPerceived.notPerceived(PerceptionFailure.NO_NAMESPACE, (PsiJavaDocumentedElement) declarationOfCollection);
        }
    }

    private void resolveBsonDocumentChain(PsiExpression expr, LinkedHashSet<MQLQuery.Predicate<PsiElement>> queryDescription, MongoDBFacade.ConnectionAwareResult<CollectionSchema> schemaOfCollection) {
        if (expr instanceof PsiNewExpression newDoc) {
            var constructorMethod = newDoc.resolveConstructor();
            var returnType = constructorMethod.getContainingClass();

            if (returnType.getQualifiedName().equals("org.bson.Document")) {
                // we are in "new Document(key, value)"
                var methodArgs = newDoc.getArgumentList().getExpressions();
                if (methodArgs.length == 2) {
                    queryDescription.add(resolveMQLQueryField(methodArgs[0], methodArgs[1], schemaOfCollection));
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
                        queryDescription.add(resolveMQLQueryField(methodArgs[0], methodArgs[1], schemaOfCollection));
                        break;
                    case "and":
                    case "or":
                    case "nor":
                        if (method.isVarArgs()) {
                            for (var arg : methodArgs) {
                                resolveBsonDocumentChain(arg, queryDescription, schemaOfCollection);
                            }
                        }
                        break;
                    default:
                }
            }

            if (leftExpr.getType().equalsToText("org.bson.Document") || leftExpr.getType().equalsToText("org.bson.conversions.Bson")) {
                resolveBsonDocumentChain(leftExpr.getQualifierExpression(), queryDescription, schemaOfCollection);
            } else if (leftExpr.getType().equalsToText("org.bson.Document") || leftExpr.getType().equalsToText("org.bson.conversions.Bson")) {
                resolveBsonDocumentChain(leftExpr.getQualifierExpression(), queryDescription, schemaOfCollection);
            }

            if (leftExpr.getCanonicalText().endsWith(".append")) {
                if (methodArgs.length == 2) {
                    queryDescription.add(resolveMQLQueryField(methodArgs[0], methodArgs[1], schemaOfCollection));
                }
            }
        }
    }

    private MQLQuery.Predicate<PsiElement> resolveMQLQueryField(PsiExpression field, PsiExpression value, MongoDBFacade.ConnectionAwareResult<CollectionSchema> currentSchema) {
        var providedTypeOnQuery = inferTypeOf(value);
        var warnings = new ArrayList<QueryWarning>();
        CollectionSchema.FieldValue expectedTypeDef = null;

        if (field instanceof PsiLiteralExpression literalExpr) {
            var fieldName = literalExpr.getValue().toString();
            if (currentSchema.connected()) {
                expectedTypeDef = currentSchema.result().ofField(fieldName);
                if (!expectedTypeDef.supportsProvidedType(providedTypeOnQuery)) {
                    warnings.add(
                            new QueryWarning(
                                    InspectionBundle.message("inspection.MQLQueryPerception.warning.fieldTypeDoesNotMatch",
                                            fieldName,
                                            Strings.join(expectedTypeDef.types(), ", "),
                                            providedTypeOnQuery)
                            )
                    );
                }
            }

            return MQLQuery.Predicate.named(field, value, fieldName, providedTypeOnQuery, expectedTypeDef.types(), warnings);
        } else {
            var resolvedField = inferConstantStringValue(field);
            if (resolvedField == null) {
                return MQLQuery.Predicate.newWildcard(field, value, warnings);
            } else {
                return MQLQuery.Predicate.named(field, value, resolvedField, providedTypeOnQuery, expectedTypeDef.types(), warnings);
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

    public static BsonType inferTypeOf(PsiExpression expression) {
        return switch (expression.getType().getCanonicalText()) {
            case "java.lang.String" -> BsonType.STRING;
            case "boolean", "java.lang.Boolean" -> BsonType.BOOLEAN;
            case "short", "java.lang.Short", "int", "java.lang.Integer" -> BsonType.INTEGER;
            case "long", "java.lang.Long", "java.math.BigInteger" -> BsonType.LONG;
            case "float", "java.lang.Float", "double", "java.lang.Double" -> BsonType.DOUBLE;
            case "java.math.BigDecimal" -> BsonType.DECIMAL;
            default -> BsonType.ANY;
        };
    }

}
