package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public abstract class ProjectionComputationNode<Origin> extends BaseNode<Origin> {
    public ProjectionComputationNode(Origin origin, List<Node<Origin>> children) {
        super(origin, children);
    }
}
