package cat.kmruiz.mongodb.lang.java.mql;

import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import cat.kmruiz.mongodb.services.mql.ast.InvalidMQLNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.aggregate.AggregateMatchStageNode;
import cat.kmruiz.mongodb.services.mql.ast.aggregate.AggregateProjectStageNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import cat.kmruiz.mongodb.services.mql.ast.projection.ExcludeFieldNode;
import cat.kmruiz.mongodb.services.mql.ast.projection.IncludeFieldNode;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import cat.kmruiz.mongodb.services.mql.ast.values.ConstantValueNode;
import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ReferenceValueNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.AndNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.NorNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.OrNode;
import com.intellij.openapi.components.Service;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class JavaMongoDBDriverMQLParser {
    public Node parse(@NotNull PsiMethodCallExpression methodCall) {
        var parent = findContainingCallChain(methodCall);

        var declarationOfCollection = findReferenceToCollection(methodCall);
        if (declarationOfCollection == null) {
            return new InvalidMQLNode(methodCall, null, InvalidMQLNode.Reason.UNKNOWN_NAMESPACE);
        }

        var mdbNamespace = inferNamespace(declarationOfCollection);
        if (!mdbNamespace.isKnown()) {
            return new InvalidMQLNode(methodCall, declarationOfCollection, InvalidMQLNode.Reason.UNKNOWN_NAMESPACE);
        }

        var methodRefCall = methodCall.getMethodExpression().getCanonicalText();
        var operation = inferOperationFromMethod(parent, methodRefCall);
        var predicates = new ArrayList<Node>();

        var maybeQueryDsl = fromArgumentListIfValid(operation, methodCall, 0);
        if (maybeQueryDsl.isEmpty()) {
            return new InvalidMQLNode(methodCall, declarationOfCollection, InvalidMQLNode.Reason.INVALID_QUERY);
        }

        for (var psiExpr : maybeQueryDsl) {
            resolveBsonDocumentChain(psiExpr, predicates);
        }

        return new QueryNode(mdbNamespace, operation, methodCall, declarationOfCollection, predicates, QueryNode.ReadPreference.PRIMARY, 0);
    }

    private void resolveBsonDocumentChain(PsiExpression expr, List<Node> predicates) {
        if (expr instanceof PsiMethodCallExpression methodCall) {
            var methodName = "";
            var methodArgs = methodCall.getArgumentList().getExpressions();
            var leftExpr = methodCall.getMethodExpression();
            var method = methodCall.resolveMethod();
            if (method == null) {
                if (!methodCall.getMethodExpression().getText().startsWith("Filters.") && !methodCall.getMethodExpression().getText().startsWith("Aggregates.")) {
                    return;
                }

                methodName = methodCall.getMethodExpression().getText();
                var qn = methodName.split("\\.");
                methodName = qn[1];
            } else {
                methodName = method.getName();
            }

            switch (methodName) {
                case "match" -> {
                    var matchPredicates = new ArrayList<Node>();
                    var filters = fromArgumentListIfValid(QueryNode.Operation.FIND_MANY, methodCall, 0);
                    if (!filters.isEmpty()) {
                        resolveBsonDocumentChain(filters.get(0), matchPredicates);
                    }
                    var stage = new AggregateMatchStageNode(methodCall, matchPredicates);
                    predicates.add(stage);
                }
                case "project" -> {
                    var projectPredicates = new ArrayList<Node>();
                    var filters = fromProjectExpression(methodCall);
                    for (var filter : filters) {
                        if (filter instanceof PsiMethodCallExpression filterCallExpr) {
                            if (filterCallExpr.getText().contains("excludeId")) {
                                projectPredicates.add(new ExcludeFieldNode(filterCallExpr, new FieldReferenceNode(filterCallExpr, "_id")));
                            } else if (filterCallExpr.getText().contains("exclude")) {
                                var filterArgument = filterCallExpr.getArgumentList().getExpressions()[0];
                                var fieldToExclude = inferConstantValue(filterArgument);
                                if (fieldToExclude == null) {
                                    projectPredicates.add(new ExcludeFieldNode(filterCallExpr, new FieldReferenceNode(filterArgument, null)));
                                } else {
                                    projectPredicates.add(new ExcludeFieldNode(filterCallExpr, new FieldReferenceNode(filterArgument, fieldToExclude.toString())));
                                }
                            } else if (filterCallExpr.getText().contains("include")) {
                                var filterArgument = filterCallExpr.getArgumentList().getExpressions()[0];
                                var fieldToInclude = inferConstantValue(filterArgument);
                                if (fieldToInclude == null) {
                                    projectPredicates.add(new IncludeFieldNode(filterCallExpr, new FieldReferenceNode(filterArgument, null)));
                                } else {
                                    projectPredicates.add(new IncludeFieldNode(filterCallExpr, new FieldReferenceNode(filterArgument, fieldToInclude.toString())));
                                }
                            }
                        }
                    }
                    var stage = new AggregateProjectStageNode(methodCall, projectPredicates);
                    predicates.add(stage);
                }
                case "eq", "ne", "gt", "lt", "gte", "lte", "in", "nin", "exists", "type", "mod", "regex", "all", "elemMatch", "size", "bitsAllClear", "bitsAllSet", "bitsAnyClear", "bitsAnySet", "geoWithin", "geoWithinBox", "geoWithinPolygon", "geoWithinCenter", "geoWithinCenterSphere", "geoIntersects", "near", "nearSphere" -> {
                    var mqlPred = resolveMQLPredicate(methodCall, methodName, methodArgs[0], methodArgs[1]);
                    predicates.add(mqlPred);
                }
                case "and", "or", "nor" -> {
                    var allPreds = new ArrayList<Node>();
                    for (var arg : methodArgs) {
                        resolveBsonDocumentChain(arg, allPreds);
                    }

                    switch (methodName) {
                        case "and" -> predicates.add(new AndNode(methodCall, allPreds));
                        case "or" -> predicates.add(new OrNode(methodCall, allPreds));
                        case "nor" -> predicates.add(new NorNode(methodCall, allPreds));
                    }
                }
                default -> {
                }
            }

            var leftExprType = leftExpr.getType();
            if (leftExprType != null) {
                if (leftExprType.equalsToText("org.bson.Document") || leftExprType.equalsToText("org.bson.conversions.Bson")) {
                    resolveBsonDocumentChain(leftExpr.getQualifierExpression(), predicates);
                } else if (leftExprType.equalsToText("org.bson.Document") || leftExprType.equalsToText("org.bson.conversions.Bson")) {
                    resolveBsonDocumentChain(leftExpr.getQualifierExpression(), predicates);
                }
            }

            if (leftExpr.getCanonicalText().endsWith(".append")) {
                if (methodArgs.length == 2) {
                    predicates.add(resolveMQLPredicate(methodCall, "eq", methodArgs[0], methodArgs[1]));
                }
            }
        }
    }

    private BinOpNode resolveMQLPredicate(PsiElement origin, String operation, PsiExpression field, PsiExpression value) {
        var resolvedConstant = inferConstantValue(value);
        var inferredType = inferTypeOf(value);
        ValueNode valueNode = null;

        if (resolvedConstant == null) {
            valueNode = new ReferenceValueNode(value, inferredType);
        } else {
            valueNode = new ConstantValueNode(value, inferredType, resolvedConstant);
        }

        if (field instanceof PsiLiteralExpression literalExpr) {
            var fieldName = literalExpr.getValue().toString();
            return new BinOpNode(origin, operation, new FieldReferenceNode(field, fieldName), Collections.singletonList(valueNode));
        } else {
            var resolvedField = inferConstantValue(field);
            if (resolvedField == null) {
                return new BinOpNode(origin, operation, new FieldReferenceNode(field, null), Collections.singletonList(valueNode));
            } else {
                return new BinOpNode(origin, operation, new FieldReferenceNode(field, resolvedField.toString()), Collections.singletonList(valueNode));
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
                if (localVar.getType().getCanonicalText().contains("MongoCollection")) {
                    return localVar;
                }
            } else if (reference instanceof PsiField classField) {
                if (classField.getType().getCanonicalText().contains("MongoCollection")) {
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

    private static QueryNode.Operation inferOperationFromMethod(PsiMethodCallExpression parent, @NotNull String methodRefCall) {
        if (methodRefCall.contains("find")) {
            var allCursorModifiers = PsiTreeUtil.collectElementsOfType(parent, PsiMethodCallExpression.class);
            var isFindOne = allCursorModifiers.stream()
                    .map(call -> call.getText().replaceAll("\\s+", ""))
                    .anyMatch(call -> call.contains("limit(1)") || call.endsWith("first()") || call.endsWith("get(0)"));

            return isFindOne ? QueryNode.Operation.FIND_ONE : QueryNode.Operation.FIND_MANY;
        } else if (methodRefCall.contains("updateOne")) {
            return QueryNode.Operation.UPDATE_ONE;
        } else if (methodRefCall.contains("updateMany")) {
            return QueryNode.Operation.UPDATE_MANY;
        } else if (methodRefCall.contains("deleteOne")) {
            return QueryNode.Operation.DELETE_ONE;
        } else if (methodRefCall.contains("deleteMany")) {
            return QueryNode.Operation.DELETE_MANY;
        } else if (methodRefCall.contains("aggregate")) {
            return QueryNode.Operation.AGGREGATE;
        } else {
            return QueryNode.Operation.UNKNOWN;
        }
    }

    private static List<PsiExpression> fromArgumentListIfValid(QueryNode.Operation operation, @NotNull PsiMethodCallExpression methodCall, int argIdx) {
        var args = methodCall.getArgumentList();

        if (operation == QueryNode.Operation.AGGREGATE) {
            var els = PsiTreeUtil.collectElements(methodCall, el -> el.getText().endsWith("asList"));
            var allExprs = PsiTreeUtil.collectElements(els[0].getNextSibling(), el -> el instanceof PsiMethodCallExpression && el.getText().startsWith("Aggregates."));
            return Arrays.stream(allExprs).map(e -> (PsiExpression) e).toList();
        } else if (args.getExpressionCount() >= argIdx) {
            var queryArg = Objects.requireNonNullElse(args.getExpressionTypes().length >= argIdx ? args.getExpressionTypes()[argIdx] : null, PsiType.getTypeByName("org.bson.Document", methodCall.getProject(), GlobalSearchScope.EMPTY_SCOPE));
            if (queryArg.isValid() && (queryArg.equalsToText("org.bson.Document") || queryArg.equalsToText("org.bson.conversions.Bson"))) {
                return Collections.singletonList(args.getExpressions()[argIdx]);
            }else {
                var allQueryExpr = findAllQueryExpr(methodCall);
                for (var idx = allQueryExpr.size() - 1; idx >= 0; idx--) {
                    var result = fromArgumentListIfValid(operation, allQueryExpr.get(idx), argIdx);
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<PsiExpression> fromProjectExpression(@NotNull PsiMethodCallExpression methodCall) {
        var allProjections = PsiTreeUtil.collectElements(methodCall, el -> el instanceof PsiMethodCallExpression && el.getText().contains("Projections") && !el.getText().contains("fields"));
        return Arrays.stream(allProjections).map(e -> (PsiExpression) e).toList();
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
                collection = inferConstantValue(collectionNameEl).toString();
                database = inferDatabaseName(methodCall.getMethodExpression().getQualifierExpression());
            }
        }

        return new MongoDBNamespace(database, collection);
    }

    private String inferDatabaseName(PsiElement el) {
        if (el instanceof PsiMethodCallExpression methodCall) {
            if (methodCall.getMethodExpression().getText().endsWith("getDatabase")) {
                var collectionNameEl = methodCall.getArgumentList().getExpressions()[0];
                return inferConstantValue(collectionNameEl).toString();
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

    private Object inferConstantValue(PsiElement expression) {
        if (expression instanceof PsiReferenceExpression refExpr) {
            var varRef = refExpr.resolve();
            return inferConstantValue(varRef);
        } else if (expression instanceof PsiLocalVariable localVar) {
            return inferConstantValue(localVar.getInitializer());
        } else if (expression instanceof PsiLiteralValue literalVal) {
            return literalVal.getValue();
        } else if (expression instanceof PsiField field) {
            return field.computeConstantValue();
        }

        return null;
    }

    private PsiMethodCallExpression findContainingCallChain(PsiMethodCallExpression current) {
        var parent = current.getParent();
        if (parent instanceof PsiReferenceExpression refExpr) {
            return (PsiMethodCallExpression) refExpr.getParent();
        }

        return current;
    }

    private static BsonType inferTypeOf(PsiExpression expression) {
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

    private static List<PsiMethodCallExpression> findAllQueryExpr(PsiElement expr) {
        var result = new ArrayList<PsiMethodCallExpression>();
        for (var child = expr.getFirstChild(); child != null; child = child.getNextSibling()) {
            result.addAll(findAllQueryExpr(child));
            if (child instanceof PsiMethodCallExpression callExpr) {
                result.add(callExpr);
            }
        }

        return result;
    }
}
