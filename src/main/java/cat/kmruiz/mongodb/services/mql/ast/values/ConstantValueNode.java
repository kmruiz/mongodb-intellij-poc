package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record ConstantValueNode<Origin>(Origin origin, BsonType type, Object constant) implements ValueNode<Origin> {
    @Override
    public Optional<Object> inferValue() {
        return Optional.of(constant);
    }

    @Override
    public List<Node<Origin>> children() {
        return Collections.emptyList();
    }
}
