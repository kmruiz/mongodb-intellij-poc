package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record ReferenceValueNode<Origin, Type>(Origin origin, Type type) implements ValueNode<Origin, Type> {
    @Override
    public List<Node<Origin>> children() {
        return Collections.emptyList();
    }

    @Override
    public Optional<Object> inferValue() {
        return Optional.empty();
    }
}
