package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.lang.java.perception.MQLQueryPerception;
import cat.kmruiz.mongodb.lang.java.quickfix.DeduceIndexQuickFix;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.ui.IndexBeautifier;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class QueryIndexingQualityInspection extends AbstractBaseJavaLocalInspectionTool {
    private final MQLQueryPerception queryPerception = new MQLQueryPerception();

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        var facade = holder.getProject().getService(MongoDBFacade.class);

        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                var owningClass = expression.resolveMethod().getContainingClass();

                if (owningClass.getQualifiedName().equals("com.mongodb.client.MongoCollection")) {
                    var perception = queryPerception.parse(expression);
                    if (!perception.hasBeenPerceived()) {
                        return;
                    }

                    handlePerception(expression, perception);
                }
            }

            @Override
            public void visitMethod(PsiMethod method) {
                var perception = queryPerception.parse(method);
                if (!perception.hasBeenPerceived()) {
                    return;
                }

                handlePerception(method, perception);
            }

            private void handlePerception(PsiElement member, MQLQueryPerception.MQLQueryOrNotPerceived perception) {
                var indexes = facade.indexesOfCollection(perception.database(), perception.collection());
                if (!indexes.connected()) {
                    return;
                }

                var query = perception.query();
                var candidateIndexes = facade.candidateIndexesForQuery(perception.database(), perception.collection(), query).result();
                var isCollectionSharded = facade.isCollectionSharded(perception.database(), perception.collection()).result();

                if (query.hasWildcardField() && query.hasHighCardinality()) {
                    holder.registerProblem(member,
                            InspectionBundle.message("inspection.QueryIndexingQualityInspection.queryMightUseTheAttributePattern",
                                    perception.database(),
                                    perception.collection()));
                } else if (candidateIndexes.isEmpty()) {
                    holder.registerProblem(member,
                            InspectionBundle.message("inspection.QueryIndexingQualityInspection.basicQueryNotCovered",
                                    perception.database(),
                                    perception.collection(),
                                    IndexBeautifier.beautify(indexes.result())),
                            new DeduceIndexQuickFix(perception.database(), perception.collection(), query));
                } else {
                    if (isCollectionSharded) {
                        var canUseShardingKey = candidateIndexes.stream().anyMatch(MQLIndex::shardKey);
                        if (!canUseShardingKey) {
                            var shardingKey = indexes.result().stream().filter(MQLIndex::shardKey).findFirst().get();

                            holder.registerProblem(member,
                                    InspectionBundle.message("inspection.QueryIndexingQualityInspection.indexIsNotShardKey",
                                            perception.database(),
                                            perception.collection(),
                                            IndexBeautifier.beautify(candidateIndexes.get(0)),
                                            IndexBeautifier.beautify(shardingKey)));
                        }
                    } else if (candidateIndexes.size() > 1){
                        holder.registerProblem(member,
                                InspectionBundle.message("inspection.QueryIndexingQualityInspection.queryCoveredByMultipleIndexes",
                                        perception.database(),
                                        perception.collection(),
                                        IndexBeautifier.beautify(candidateIndexes)));
                    }
                }
            }
        };
    }
}
