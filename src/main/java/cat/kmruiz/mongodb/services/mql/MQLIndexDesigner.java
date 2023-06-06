package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class MQLIndexDesigner {
    private final MongoDBFacade facade;

    public MQLIndexDesigner(Project project) {
        this.facade = project.getService(MongoDBFacade.class);
    }

    public MQLIndex designIndexForQuery(QueryNode<?> queryNode) {
        var equality = new HashSet<String>();
        var sort = new HashSet<String>();
        var range = new HashSet<String>();

        splitIntoESR(queryNode, equality, sort, range);

        var indexDef = new LinkedList<MQLIndex.MQLIndexField>();
        for (var e : equality) {
            indexDef.add(new MQLIndex.MQLIndexField(e, MQLIndex.MQLIndexType.ASC));
        }

        for (var s : sort) {
            indexDef.add(new MQLIndex.MQLIndexField(s, MQLIndex.MQLIndexType.ASC));
        }

        for (var r : range) {
            indexDef.add(new MQLIndex.MQLIndexField(r, MQLIndex.MQLIndexType.ASC));
        }

        return new MQLIndex("", indexDef, false);
    }

    private void splitIntoESR(Node<?> node, HashSet<String> equality, HashSet<String> sort, HashSet<String> range) {
        if (node instanceof BinOpNode<?> binOp) {
            if (RANGE_OPS.contains(binOp.operation())) {
                range.add(binOp.field());
            } else {
                equality.add(binOp.field());
            }
        } else {
            for (var child : node.children()) {
                splitIntoESR(child, equality, sort, range);
            }
        }
    }

    private static final List<String> RANGE_OPS = List.of(
            "gt", "gte", "lt", "lte"
    );
}
