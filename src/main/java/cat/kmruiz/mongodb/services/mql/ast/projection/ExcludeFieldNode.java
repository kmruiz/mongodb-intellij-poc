package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;

import java.util.List;

public class ExcludeFieldNode<Origin> extends ProjectionComputationNode<Origin> {
    private final FieldReferenceNode<Origin> reference;

    public ExcludeFieldNode(Origin origin, FieldReferenceNode<Origin> reference) {
        super(origin, List.of(reference));

        this.reference = reference;
    }

    public FieldReferenceNode<Origin> reference() {
        return reference;
    }
}
