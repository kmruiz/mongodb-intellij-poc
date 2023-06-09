package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;

import java.util.Collections;
import java.util.List;

public record ExcludeFieldNode<Origin>(Origin origin, FieldReferenceNode<Origin> reference) implements ProjectionComputationNode<Origin> {
    @Override
    public List<Node<Origin>> children() {
        return Collections.singletonList(reference);
    }
}
