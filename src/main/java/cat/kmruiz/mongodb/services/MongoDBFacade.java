package cat.kmruiz.mongodb.services;

import cat.kmruiz.mongodb.services.config.MongoDBConnectionConfiguration;
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

    public ConnectionAwareResult<List<Document>> indexesOfCollection(String database, String collection) {
        if (!assertConnection()) {
            return ConnectionAwareResult.disconnected();
        }

        var coll = this.client.getDatabase(database).getCollection(collection);
        var result = coll.listIndexes().into(new ArrayList<>());
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
