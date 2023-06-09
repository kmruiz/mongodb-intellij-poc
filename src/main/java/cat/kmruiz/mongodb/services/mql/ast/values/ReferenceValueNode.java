package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;

import java.util.Collections;
import java.util.Optional;

public final class ReferenceValueNode<Origin> extends ValueNode<Origin> {
    public ReferenceValueNode(Origin origin, BsonType type) {
        super(origin, Collections.emptyList(), type);
    }

    @Override
    public Optional<Object> inferValue() {
        return Optional.empty();
    }
}
