package cat.kmruiz.mongodb.services.mql.ast.aggregate;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public final class AggregateProjectStageNode<Origin> extends BaseNode<Origin> {
    public AggregateProjectStageNode(Origin origin, List<Node<Origin>> children) {
        super(origin, children);
    }
}
