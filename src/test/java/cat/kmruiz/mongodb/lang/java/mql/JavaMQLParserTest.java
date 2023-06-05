package cat.kmruiz.mongodb.lang.java.mql;

import cat.kmruiz.mongodb.services.mql.ast.InvalidMQLNode;
import cat.kmruiz.mongodb.services.mql.ast.PredicateNode;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ReferenceValueNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.AndNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.NorNode;
import cat.kmruiz.mongodb.services.mql.ast.varops.OrNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.RunsInEdt;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5;
import com.intellij.testFramework.fixtures.impl.ModuleFixtureBuilderImpl;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static cat.kmruiz.mongodb.services.mql.ast.QueryNode.Operation.FIND_MANY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunsInEdt
public class JavaMQLParserTest extends LightJavaCodeInsightFixtureTestCase5 {
    private static final String DATABASE = "data";
    private static final String COLLECTION = "coll";

    private final JavaMQLParser javaMQLParser = new JavaMQLParser();

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

        var predicate = (PredicateNode<PsiElement, Object>) result.children().get(0);
        assertEquals("a", predicate.field());
        assertEquals(1, predicate.value().inferValue().get());
    }

    @Test
    void should_understand_basic_filter_and_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.and(Filters.eq(\"a\", 1), Filters.eq(\"b\", 2)))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (AndNode<PsiElement>) result.children().get(0);
        var firstCond = (PredicateNode<PsiElement, Object>) predicate.children().get(0);
        var secondCond = (PredicateNode<PsiElement, Object>) predicate.children().get(1);

        assertEquals("a", firstCond.field());
        assertEquals(1, firstCond.value().inferValue().get());

        assertEquals("b", secondCond.field());
        assertEquals(2, secondCond.value().inferValue().get());
    }

    @Test
    void should_understand_basic_filter_or_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.or(Filters.eq(\"a\", 1), Filters.eq(\"b\", 2)))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (OrNode<PsiElement>) result.children().get(0);
        var firstCond = (PredicateNode<PsiElement, Object>) predicate.children().get(0);
        var secondCond = (PredicateNode<PsiElement, Object>) predicate.children().get(1);

        assertEquals("a", firstCond.field());
        assertEquals(1, firstCond.value().inferValue().get());

        assertEquals("b", secondCond.field());
        assertEquals(2, secondCond.value().inferValue().get());
    }

    @Test
    void should_understand_basic_filter_nor_queries() {
        var result = parseValid(withJavaQuery("collection.find(Filters.nor(Filters.eq(\"a\", 1), Filters.eq(\"b\", 2)))"));

        assertEquals(DATABASE, result.namespace().database());
        assertEquals(COLLECTION, result.namespace().collection());
        assertEquals(FIND_MANY, result.operation());

        var predicate = (NorNode<PsiElement>) result.children().get(0);
        var firstCond = (PredicateNode<PsiElement, Object>) predicate.children().get(0);
        var secondCond = (PredicateNode<PsiElement, Object>) predicate.children().get(1);

        assertEquals("a", firstCond.field());
        assertEquals(1, firstCond.value().inferValue().get());

        assertEquals("b", secondCond.field());
        assertEquals(2, secondCond.value().inferValue().get());
    }

    private QueryNode<PsiElement> parseValid(PsiMethodCallExpression methodCallExpression) {
        var result = javaMQLParser.parse(methodCallExpression);
        if (result instanceof QueryNode<PsiElement> qn) {
            return qn;
        }

        var invalidNode = (InvalidMQLNode<?>) result;
        throw new IllegalArgumentException(invalidNode.reason().toString());
    }
    private PsiMethodCallExpression withJavaQuery(String javaQuery) {
        var javaClassName = getTestName(false);
        var javaCode = """
                import com.mongodb.client.MongoClient;
                import com.mongodb.client.MongoClients;
                import com.mongodb.client.model.Aggregates;
                import com.mongodb.client.model.Filters;
                                
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
}
