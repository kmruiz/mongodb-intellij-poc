package cat.kmruiz.mongodb.services.mql.ast.binops;

import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;

import java.util.List;

public record BinOpNode<Origin>(Origin origin, String operation, FieldReferenceNode<Origin> field, List<Node<Origin>> children) implements Node<Origin> {
}
