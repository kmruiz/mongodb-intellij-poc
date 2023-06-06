package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.lang.java.quickfix.AddJavaDocForNamespace;
import cat.kmruiz.mongodb.lang.java.quickfix.DeduceIndexQuickFix;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.ast.InvalidMQLNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import cat.kmruiz.mongodb.ui.IndexBeautifier;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaDocumentedElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service(Service.Level.PROJECT)
public final class MQLIndexQualityChecker implements MQLQueryQualityChecker {
    private final MongoDBFacade mongoDBFacade;
    private final MQLIndexDesigner mqlIndexDesigner;

    public MQLIndexQualityChecker(Project project) {
        this.mongoDBFacade = project.getService(MongoDBFacade.class);
        this.mqlIndexDesigner = project.getService(MQLIndexDesigner.class);
    }

    public void check(QueryNode<PsiElement> query, ProblemsHolder holder) {
        var indexResult = mongoDBFacade.indexesOfCollection(query.namespace());
        var shardingResult = mongoDBFacade.isCollectionSharded(query.namespace());

        if (!indexResult.connected() || !shardingResult.connected()) {
            return;
        }

        var isShardedCollection = shardingResult.result();
        var allUsedFields = collectAllFieldNames(query);
        var usableIndexes = usableIndexesForFields(indexResult.result(), allUsedFields);

        if (usableIndexes.size() == 0) {
            holder.registerProblem(query.origin(),
                    InspectionBundle.message("inspection.QueryIndexingQualityInspection.basicQueryNotCovered",
                            query.namespace().database(),
                            query.namespace().collection(),
                            IndexBeautifier.beautify(indexResult.result())),
                    new DeduceIndexQuickFix(mongoDBFacade, query.namespace(), mqlIndexDesigner.designIndexForQuery(query))
            );
        } else if (usableIndexes.size() > 1) {
            if (isShardedCollection) {
                boolean canUseShardingKey = usableIndexes.stream().anyMatch(MQLIndex::shardKey);
                if (!canUseShardingKey) {
                    var shardingKey = indexResult.result().stream().filter(MQLIndex::shardKey).findFirst().get();

                    holder.registerProblem(query.origin(),
                            InspectionBundle.message("inspection.QueryIndexingQualityInspection.indexIsNotShardKey",
                                    query.namespace().database(),
                                    query.namespace().collection(),
                                    IndexBeautifier.beautify(usableIndexes.get(0)),
                                    IndexBeautifier.beautify(shardingKey))
                    );
                }
            } else {
                holder.registerProblem(query.origin(),
                        InspectionBundle.message("inspection.QueryIndexingQualityInspection.queryCoveredByMultipleIndexes",
                                query.namespace().database(),
                                query.namespace().collection(),
                                IndexBeautifier.beautify(usableIndexes)));
            }
        }
    }

    @Override
    public void checkInvalid(InvalidMQLNode<PsiElement> invalid, ProblemsHolder holder) {
        if (invalid.reason() == InvalidMQLNode.Reason.UNKNOWN_NAMESPACE && invalid.collectionReference() != null) {
            holder.registerProblem(invalid.collectionReference(),
                    InspectionBundle.message("inspection.QueryIndexingQualityInspection.couldNotDetectNamespace"),
                    new AddJavaDocForNamespace((PsiJavaDocumentedElement) invalid.collectionReference()));
        }
    }

    private Set<String> collectAllFieldNames(Node<PsiElement> node) {
        var fieldNames = new HashSet<String>();
        if (node instanceof BinOpNode<PsiElement> binOp) {
            fieldNames.add(binOp.field());
        } else {
            for (var child : node.children()) {
                fieldNames.addAll(collectAllFieldNames(child));
            }
        }

        return fieldNames;
    }

    private List<MQLIndex> usableIndexesForFields(List<MQLIndex> allIndexes, Set<String> fields) {
        return allIndexes.stream().filter(index ->
                index.definition().stream().anyMatch(field -> fields.contains(field.fieldName()))
        ).toList();
    }
}
