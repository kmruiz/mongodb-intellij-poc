package cat.kmruiz.mongodb.services.mql.ast.varops;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public final class AndNode<Origin> extends BaseNode<Origin> {
    public AndNode(Origin origin, List<Node<Origin>> children) {
        super(origin, children);
    }
}
