package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record ConstantValueNode<Origin, Type>(Origin origin, Type type, Object constant) implements ValueNode<Origin, Type> {
    @Override
    public Optional<Object> inferValue() {
        return Optional.of(constant);
    }

    @Override
    public List<Node<Origin>> children() {
        return Collections.emptyList();
    }
}
