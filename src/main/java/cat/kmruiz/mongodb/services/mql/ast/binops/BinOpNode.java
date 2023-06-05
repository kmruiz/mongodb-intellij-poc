package cat.kmruiz.mongodb.services.mql.ast.binops;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public record BinOpNode<Origin>(Origin origin, String operation, String field, Origin fieldOrigin, List<Node<Origin>> children) implements Node<Origin> {
}
