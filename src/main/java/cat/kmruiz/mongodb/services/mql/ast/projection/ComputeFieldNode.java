package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;
import com.intellij.psi.PsiElement;

import java.util.List;

public final class ComputeFieldNode extends ProjectionComputationNode {
    private final FieldReferenceNode reference;
    private final ValueNode value;

    public ComputeFieldNode(PsiElement origin, FieldReferenceNode reference, ValueNode value) {
        super(origin, List.of(reference, value));

        this.reference = reference;
        this.value = value;
    }

    public FieldReferenceNode reference() {
        return reference;
    }

    public ValueNode value() {
        return value;
    }
}
