package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;

import java.util.List;

public class IncludeFieldNode<Origin> extends ProjectionComputationNode<Origin> {
    private final FieldReferenceNode<Origin> reference;

    public IncludeFieldNode(Origin origin, FieldReferenceNode<Origin> reference) {
        super(origin, List.of(reference));

        this.reference = reference;
    }

    public FieldReferenceNode<Origin> reference() {
        return reference;
    }
}