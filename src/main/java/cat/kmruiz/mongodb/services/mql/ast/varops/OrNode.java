package cat.kmruiz.mongodb.services.mql.ast.varops;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public final class OrNode<Origin> extends BaseNode<Origin> {
    public OrNode(Origin origin, List<Node<Origin>> children) {
        super(origin, children);
    }
}