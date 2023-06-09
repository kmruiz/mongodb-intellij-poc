package cat.kmruiz.mongodb.services.mql.ast.binops;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;
import com.intellij.psi.PsiElement;

import java.util.List;

public final class BinOpNode extends BaseNode {
    private final String operation;
    private final FieldReferenceNode field;

    public BinOpNode(PsiElement origin, String operation, FieldReferenceNode field, List<Node> children) {
        super(origin, children);
        this.operation = operation;
        this.field = field;
    }

    public String operation() {
        return operation;
    }

    public FieldReferenceNode field() {
        return field;
    }
}
