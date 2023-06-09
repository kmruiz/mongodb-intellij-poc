package cat.kmruiz.mongodb.services.mql.ast.binops;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.values.FieldReferenceNode;

import java.util.List;

public final class BinOpNode<Origin> extends BaseNode<Origin> {
    private final String operation;
    private final FieldReferenceNode<Origin> field;

    public BinOpNode(Origin origin, String operation, FieldReferenceNode<Origin> field, List<Node<Origin>> children) {
        super(origin, children);
        this.operation = operation;
        this.field = field;
    }

    public String operation() {
        return operation;
    }

    public FieldReferenceNode<Origin> field() {
        return field;
    }
}
