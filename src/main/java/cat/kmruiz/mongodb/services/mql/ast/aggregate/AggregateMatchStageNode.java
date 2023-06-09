package cat.kmruiz.mongodb.services.mql.ast.aggregate;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public final class AggregateMatchStageNode<Origin> extends BaseNode<Origin> {
    public AggregateMatchStageNode(Origin origin, List<Node<Origin>> children) {
        super(origin, children);
    }
}
