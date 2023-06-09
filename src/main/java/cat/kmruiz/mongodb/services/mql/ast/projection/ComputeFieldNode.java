package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;

import java.util.Collections;
import java.util.List;

public record ComputeFieldNode<Origin>(Origin origin, FieldReferenceNode<Origin> reference, ValueNode<Origin> value) implements ProjectionComputationNode<Origin> {
    @Override
    public List<Node<Origin>> children() {
        return List.of(reference, value);
    }
}
