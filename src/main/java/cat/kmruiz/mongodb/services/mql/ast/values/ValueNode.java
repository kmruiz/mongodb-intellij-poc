package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;

import java.util.List;
import java.util.Optional;

public abstract class ValueNode<Origin> extends BaseNode<Origin> {
    private final BsonType type;

    protected ValueNode(Origin origin, List<Node<Origin>> children, BsonType type) {
        super(origin, children);
        this.type = type;
    }

    public BsonType type() {
        return type;
    }
    public abstract Optional<Object> inferValue();
}
