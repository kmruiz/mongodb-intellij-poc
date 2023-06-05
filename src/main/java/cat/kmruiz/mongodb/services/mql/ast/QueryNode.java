package cat.kmruiz.mongodb.services.mql.ast;

import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;

import java.util.List;

public record QueryNode<Origin>(
        MongoDBNamespace namespace,
        Operation operation,
        Origin origin,
        List<Node<Origin>> children,
        ReadPreference readPreference,
        int maxStaleness
) implements Node<Origin> {
    public enum Operation {
        FIND_ONE, FIND_MANY, UPDATE_ONE, UPDATE_MANY, DELETE_ONE, DELETE_MANY, UNKNOWN
    }

    public enum ReadPreference {
        PRIMARY, PRIMARY_PREFERRED, SECONDARY, SECONDARY_PREFERRED, NEAREST
    }
}
