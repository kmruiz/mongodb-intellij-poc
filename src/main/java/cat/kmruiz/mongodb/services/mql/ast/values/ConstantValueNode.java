package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;

import java.util.Collections;
import java.util.Optional;

public final class ConstantValueNode<Origin> extends ValueNode<Origin> {
    private final Object constant;

    public ConstantValueNode(Origin origin, BsonType type, Object constant) {
        super(origin, Collections.emptyList(), type);
        this.constant = constant;
    }

    @Override
    public Optional<Object> inferValue() {
        return Optional.of(constant);
    }
}
