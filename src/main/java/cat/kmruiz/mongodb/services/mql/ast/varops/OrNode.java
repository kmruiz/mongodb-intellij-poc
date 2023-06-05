package cat.kmruiz.mongodb.services.mql.ast.varops;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public record OrNode<Origin>(Origin origin, List<Node<Origin>> children) implements Node<Origin> {
}
