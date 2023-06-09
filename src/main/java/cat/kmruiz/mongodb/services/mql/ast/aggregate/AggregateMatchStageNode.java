package cat.kmruiz.mongodb.services.mql.ast.aggregate;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.Collections;
import java.util.List;

public record AggregateMatchStageNode<Origin>(Origin origin, List<Node<Origin>> children) implements Node<Origin> {

}
