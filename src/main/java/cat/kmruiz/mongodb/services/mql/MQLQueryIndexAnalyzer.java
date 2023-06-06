package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service(Service.Level.PROJECT)
public final class MQLQueryIndexAnalyzer {
    private final MongoDBFacade facade;

    public MQLQueryIndexAnalyzer(Project project) {
        this.facade = project.getService(MongoDBFacade.class);
    }

    public List<MQLIndex> indexesOfQuery(QueryNode<?> query) {
        var indexResult = facade.indexesOfCollection(query.namespace());
        if (!indexResult.connected()) {
            return Collections.emptyList();
        }

        var allUsedFields = collectAllFieldNames(query);
        return usableIndexesForFields(indexResult.result(), allUsedFields);
    }

    private Set<String> collectAllFieldNames(Node<?> node) {
        var fieldNames = new HashSet<String>();
        if (node instanceof BinOpNode<?> binOp) {
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
