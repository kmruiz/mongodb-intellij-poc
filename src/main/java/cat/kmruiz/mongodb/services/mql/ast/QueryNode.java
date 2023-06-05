package cat.kmruiz.mongodb.services.mql.ast;

import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public record QueryNode<Origin>(
        MongoDBNamespace namespace,
        Operation operation,
        Origin origin,
        List<Node<Origin>> children
) implements Node<Origin> {
    public enum Operation {
        FIND_ONE, FIND_MANY, UPDATE_ONE, UPDATE_MANY, DELETE_ONE, DELETE_MANY, UNKNOWN
    }
}
