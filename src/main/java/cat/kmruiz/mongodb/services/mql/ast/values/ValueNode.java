package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;

import java.util.Optional;

public interface ValueNode<Origin> extends Node<Origin> {
    BsonType type();
    Optional<Object> inferValue();
}
