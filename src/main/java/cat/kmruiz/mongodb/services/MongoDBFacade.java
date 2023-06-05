package cat.kmruiz.mongodb.services;

import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MQLQuery;
import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import cat.kmruiz.mongodb.services.schema.CollectionSchema;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class MongoDBFacade {
    public record ConnectionAwareResult<T>(T result, boolean connected) {
        public static <T> ConnectionAwareResult<T> disconnected() {
            return new ConnectionAwareResult<>(null, false);
        }

        public static <T> ConnectionAwareResult<T> resulting(T result) {
            return new ConnectionAwareResult<>(result, true);
        }
    }

    private final Project currentProject;
    private MongoClient client;
    private MongoDBConfigurationResolver configurationResolver;

    public MongoDBFacade(Project project) {
        this.currentProject = project;
    }

    public <Node> ConnectionAwareResult<List<MQLIndex>> candidateIndexesForQuery(String database, String collection, MQLQuery<Node> query) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var allIndexes = indexesOfCollection(database, collection).result();
        var candidateIndexes = new ArrayList<MQLIndex>();

        for (var index : allIndexes) {
            index:
            for (var indexField : index.definition()) {
                for (MQLQuery.Predicate<Node> field : query.predicates()) {
                    if (field.wildcardField()) {
                        continue;
                    }

                    if (indexField.fieldName().equals(field.fieldName())) {
                        candidateIndexes.add(index);
                        break index;
                    }
                }
            }
        }

        candidateIndexes.sort((a, b) -> {
            if (a.shardKey()) {
                return -1;
            } else if (b.shardKey()) {
                return 1;
            } else {
                return Integer.compare(b.definition().size(), a.definition().size());
            }
        });
        return ConnectionAwareResult.resulting(candidateIndexes);
    }

    public ConnectionAwareResult<List<MQLIndex>> indexesOfCollection(String database, String collection) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var coll = this.client.getDatabase(database).getCollection(collection);
        var shardingColl = this.client.getDatabase("config").getCollection("collections");
        var shardInfo = shardingColl.find(Filters.eq("_id", "%s.%s".formatted(database, collection))).limit(1).into(new ArrayList<>(1));
        var indexList = coll.listIndexes();

        if (shardInfo.isEmpty()) {
            var result = indexList.map(MQLIndex::parseIndex).into(new ArrayList<>());
            return ConnectionAwareResult.resulting(result);
        } else {
            var shardKey = MQLIndex.parseIndex(shardInfo.get(0));
            var result = indexList.map(MQLIndex::parseIndex).map(index -> index.isSameAs(shardKey) ? index.markAsShardingKey() : index).into(new ArrayList<>());
            return ConnectionAwareResult.resulting(result);
        }

    }

    public ConnectionAwareResult<Boolean> isCollectionSharded(String database, String collection) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var shardingColl = this.client.getDatabase("config").getCollection("collections");
        var shardInfo = shardingColl.find(Filters.eq("_id", "%s.%s".formatted(database, collection))).limit(1).into(new ArrayList<>(1));

        return ConnectionAwareResult.resulting(!shardInfo.isEmpty());
    }

    public ConnectionAwareResult<CollectionSchema> schemaOf(MongoDBNamespace namespace) {
        return schemaOf(namespace.database(), namespace.collection());
    }

    public ConnectionAwareResult<CollectionSchema> schemaOf(String database, String collection) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var indexes = indexesOfCollection(database, collection).result();
        var indexedFields = new HashSet<String>();

        for (var idx : indexes) {
            indexedFields.addAll(idx.definition().stream().map(MQLIndex.MQLIndexField::fieldName).toList());
        }

        var coll = this.client.getDatabase(database).getCollection(collection);
        var resultDocs = coll.aggregate(Collections.singletonList(Aggregates.sample(500))).batchSize(500).into(new ArrayList<>());
        var schema = new CollectionSchema(database, collection, new HashMap<>());
        for (var doc : resultDocs) {
            schema = schema.merge(indexedFields, doc);
        }

        return ConnectionAwareResult.resulting(schema);
    }

    private boolean assertOfflineMode() {
        if (this.configurationResolver == null) {
            this.configurationResolver = currentProject.getService(MongoDBConfigurationResolver.class);
        }

        if (this.client == null) {
            var configuration = this.configurationResolver.getMongoDBConnectionConfiguration();
            if (!configuration.isConfigured()) {
                return true;
            }

            this.client = MongoClients.create(new ConnectionString(configuration.url()));
        }

        return false;
    }
}
