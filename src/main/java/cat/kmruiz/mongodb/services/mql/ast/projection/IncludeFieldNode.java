package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;
import com.intellij.psi.PsiElement;

import java.util.List;

public class IncludeFieldNode extends ProjectionComputationNode {
    private final FieldReferenceNode reference;

    public IncludeFieldNode(PsiElement origin, FieldReferenceNode reference) {
        super(origin, List.of(reference));

        this.reference = reference;
    }

    public FieldReferenceNode reference() {
        return reference;
    }
}