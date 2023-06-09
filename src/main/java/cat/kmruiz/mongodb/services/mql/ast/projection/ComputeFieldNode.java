package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;

import java.util.List;

public final class ComputeFieldNode<Origin> extends ProjectionComputationNode<Origin> {
    private final FieldReferenceNode<Origin> reference;
    private final ValueNode<Origin> value;

    public ComputeFieldNode(Origin origin, FieldReferenceNode<Origin> reference, ValueNode<Origin> value) {
        super(origin, List.of(reference, value));

        this.reference = reference;
        this.value = value;
    }

    public FieldReferenceNode<Origin> reference() {
        return reference;
    }

    public ValueNode<Origin> value() {
        return value;
    }
}
