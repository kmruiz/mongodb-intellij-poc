package cat.kmruiz.mongodb.services.mql.ast.varops;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public final class NorNode<Origin> extends BaseNode<Origin> {
      public NorNode(Origin origin, List<Node<Origin>> children) {
        super(origin, children);
    }
}
