package cat.kmruiz.mongodb.services;

import cat.kmruiz.mongodb.services.config.MongoDBConnectionConfiguration;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MQLQuery;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

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

    public ConnectionAwareResult<List<MQLIndex>> candidateIndexesForQuery(String database, String collection, MQLQuery query) {
        if (!assertConnection()) {
            return ConnectionAwareResult.disconnected();
        }

        var allIndexes = indexesOfCollection(database, collection).result();
        var candidateIndexes = new ArrayList<MQLIndex>();

        for (var index : allIndexes) {
            index: for (var indexField : index.definition()) {
                for (MQLQuery.MQLQueryField field : query.fields()) {
                    if (field.wildcard()) {
                        continue;
                    }

                    if (indexField.fieldName().equals(field.fieldName())) {
                        candidateIndexes.add(index);
                        break index;
                    }
                }
            }
        }

        return ConnectionAwareResult.resulting(candidateIndexes);
    }

    public ConnectionAwareResult<List<MQLIndex>> indexesOfCollection(String database, String collection) {
        if (!assertConnection()) {
            return ConnectionAwareResult.disconnected();
        }

        var coll = this.client.getDatabase(database).getCollection(collection);
        var result = coll.listIndexes().map(MQLIndex::parseIndex).into(new ArrayList<>());
        return ConnectionAwareResult.resulting(result);
    }

    private boolean assertConnection() {
        if (this.configurationResolver == null) {
            this.configurationResolver = currentProject.getService(MongoDBConfigurationResolver.class);
        }

        if (this.client == null) {
            var configuration = this.configurationResolver.getMongoDBConnectionConfiguration();
            if (!configuration.isConfigured()) {
                return false;
            }

            this.client = MongoClients.create(new ConnectionString(configuration.url()));
        }

        return true;
    }
}
