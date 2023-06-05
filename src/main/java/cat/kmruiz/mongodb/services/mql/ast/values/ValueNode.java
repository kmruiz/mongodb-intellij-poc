package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.Optional;

public interface ValueNode<Origin, Type> extends Node<Origin> {
    Type type();
    Optional<Object> inferValue();
}
