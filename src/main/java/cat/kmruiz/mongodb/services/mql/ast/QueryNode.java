package cat.kmruiz.mongodb.services.mql.ast;

import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;

import java.util.List;

public record QueryNode<Origin>(
        MongoDBNamespace namespace,
        Operation operation,
        Origin origin,
        Origin collectionOrigin,
        List<Node<Origin>> children,
        ReadPreference readPreference,
        int maxStaleness
) implements Node<Origin> {
    public enum Operation {
        FIND_ONE(false),
        FIND_MANY(true),
        UPDATE_ONE(false),
        UPDATE_MANY(true),
        DELETE_ONE(false),
        DELETE_MANY(true),
        AGGREGATE(true),
        UNKNOWN(true);

        private final boolean multiple;

        Operation(boolean multiple) {
            this.multiple = multiple;
        }

        public boolean isMultiple() {
            return multiple;
        }
    }

    public enum ReadPreference {
        PRIMARY, PRIMARY_PREFERRED, SECONDARY, SECONDARY_PREFERRED, NEAREST
    }
}
