package cat.kmruiz.mongodb.lang.java.mql;

import cat.kmruiz.mongodb.services.mql.ast.InvalidMQLNode;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.aggregate.AggregateMatchStageNode;
import cat.kmruiz.mongodb.services.mql.ast.aggregate.AggregateProjectStageNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import cat.kmruiz.mongodb.services.mql.ast.projection.ExcludeFieldNode;
import cat.kmruiz.mongodb.services.mql.ast.projection.IncludeFieldNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.AndNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.NorNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.OrNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.RunsInEdt;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static cat.kmruiz.mongodb.services.mql.ast.QueryNode.Operation.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunsInEdt
public class JavaMongoDBDriverMQLParserTest extends LightJavaCodeInsightFixtureTestCase5 {
    private static final String DATABASE = "data";
    private static final String COLLECTION = "coll";

    private final JavaMongoDBDriverMQLParser javaMongoDBDriverMQLParser = new JavaMongoDBDriverMQLParser();

    @Nullable
    @Override
    protected String getTestDataPath() {
        return "/tmp";
    }

    @Test
    void should_understand_basic_filter_eq_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.eq(\"a\", 1))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (BinOpNode) result.children().get(0);
        assertEquals("a", predicate.field().name());
        assertEquals(1, valueOfBinOp(predicate));
    }

    @Test
    void should_understand_basic_filter_and_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.and(Filters.eq(\"a\", 1), Filters.eq(\"b\", 2)))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (AndNode) result.children().get(0);
        var firstCond = (BinOpNode) predicate.children().get(0);
        var secondCond = (BinOpNode) predicate.children().get(1);

        assertEquals("a", firstCond.field().name());
        assertEquals(1, valueOfBinOp(firstCond));

        assertEquals("b", secondCond.field().name());
        assertEquals(2, valueOfBinOp(secondCond));
    }

    @Test
    void should_understand_find_one_queries_with_limit() {
        var result = parseValid(withJavaQuery("collection.find(Filters.eq(\"a\", 1)).limit(1)"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_ONE, result.operation());
    }

    @Test
    void should_understand_find_one_queries_with_first() {
        var result = parseValid(withJavaQuery("collection.find(Filters.eq(\"a\", 1)).first()"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_ONE, result.operation());
    }

    @Test
    void should_understand_basic_filter_or_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.or(Filters.eq(\"a\", 1), Filters.eq(\"b\", 2)))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (OrNode) result.children().get(0);
        var firstCond = (BinOpNode) predicate.children().get(0);
        var secondCond = (BinOpNode) predicate.children().get(1);

        assertEquals("a", firstCond.field().name());
        assertEquals(1, valueOfBinOp(firstCond));

        assertEquals("b", secondCond.field().name());
        assertEquals(2, valueOfBinOp(secondCond));
    }

    @Test
    void should_understand_basic_filter_nor_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.nor(Filters.eq(\"a\", 1), Filters.eq(\"b\", 2)))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (NorNode) result.children().get(0);
        var firstCond = (BinOpNode) predicate.children().get(0);
        var secondCond = (BinOpNode) predicate.children().get(1);

        assertEquals("a", firstCond.field().name());
        assertEquals(1, valueOfBinOp(firstCond));

        assertEquals("b", secondCond.field().name());
        assertEquals(2, valueOfBinOp(secondCond));
    }

    @Test
    void should_understand_aggregation_match() {
        var result = parseValid(withJavaQuery("collection.aggregate(Arrays.asList(Aggregates.match(Filters.eq(\"a\", 1))))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(AGGREGATE, result.operation());

        var stage = (AggregateMatchStageNode) result.children().get(0);
        var query = (BinOpNode) stage.children().get(0);

        assertEquals("a", query.field().name());
        assertEquals(1, valueOfBinOp(query));
    }

    @Test
    void should_understand_projection_stages() {
        var result = parseValid(withJavaQuery(
                """
                        collection.aggregate(
                            Arrays.asList(
                                Aggregates.project(
                                    Projections.fields(
                                        Projections.excludeId(),
                                        Projections.include("alias")
                                    )
                                )
                            )
                        )"""
        ));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(AGGREGATE, result.operation());

        var stage = (AggregateProjectStageNode) result.children().get(0);
        var excludeId = (ExcludeFieldNode) stage.children().get(0);
        var includeAlias = (IncludeFieldNode) stage.children().get(1);

        assertEquals("_id", excludeId.reference().name());
        assertEquals("alias", includeAlias.reference().name());
    }

    @Test
    void should_understand_multiple_aggregate_stages() {
        var result = parseValid(withJavaQuery("collection.aggregate(Arrays.asList(Aggregates.match(Filters.eq(\"a\", 1)), Aggregates.match(Filters.eq(\"b\", 2))))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(AGGREGATE, result.operation());

        var firstMatch = (AggregateMatchStageNode) result.children().get(0);
        var firstQuery = (BinOpNode) firstMatch.children().get(0);

        var secondMatch = (AggregateMatchStageNode) result.children().get(1);
        var secondQuery = (BinOpNode) secondMatch.children().get(0);

        assertEquals("a", firstQuery.field().name());
        assertEquals(1, valueOfBinOp(firstQuery));

        assertEquals("b", secondQuery.field().name());
        assertEquals(2, valueOfBinOp(secondQuery));
    }

    private QueryNode parseValid(PsiMethodCallExpression methodCallExpression) {
        var result = javaMongoDBDriverMQLParser.parse(methodCallExpression);
        if (result instanceof QueryNode qn) {
            return qn;
        }

        var invalidNode = (InvalidMQLNode) result;
        throw new IllegalArgumentException(invalidNode.reason().toString());
    }

    private PsiMethodCallExpression withJavaQuery(String javaQuery) {
        var javaClassName = getTestName(false);
        var javaCode = """
                import com.mongodb.client.MongoClient;
                import com.mongodb.client.MongoClients;
                import com.mongodb.client.MongoCollection;
                import com.mongodb.client.MongoDatabase;
                import com.mongodb.ExplainVerbosity;
                import com.mongodb.client.model.Accumulators;
                import com.mongodb.client.model.Aggregates;
                import com.mongodb.client.model.Filters;
                import com.mongodb.client.model.Projections;
                import org.bson.Document;
                import java.util.Arrays;
                                
                public class %s {
                    public static void main(String[] args) {
                        MongoClient client = MongoClients.create();
                        MongoCollection<Document> collection = client.getDatabase("data").getCollection("coll");
                        %s;
                    }
                }""".formatted(javaClassName, javaQuery);

        var psiRoot = getFixture().configureByText(javaClassName + ".java", javaCode);
        var elements = PsiTreeUtil.collectElements(psiRoot, element ->
                PsiTreeUtil.instanceOf(element, PsiMethodCallExpression.class) &&
                        ((PsiMethodCallExpression) element).getMethodExpression().getText().endsWith(
                                javaQuery.substring(0, javaQuery.indexOf('('))
                        ));

        return (PsiMethodCallExpression) elements[0];
    }

    @NotNull
    private static Object valueOfBinOp(BinOpNode predicate) {
        return ((ValueNode) predicate.children().get(0)).inferValue().get();
    }
}
