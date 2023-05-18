package cat.kmruiz.mongodb.services;

import cat.kmruiz.mongodb.services.config.MongoDBConnectionConfiguration;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service(Service.Level.PROJECT)
public final class MongoDBConfigurationResolver {
    private final Project currentProject;

    public MongoDBConfigurationResolver(Project project) {
        this.currentProject = project;
    }

    public MongoDBConnectionConfiguration getMongoDBConnectionConfiguration() {
        var currentFilePath = Path.of(Objects.requireNonNull(this.currentProject.getBasePath()), ".mongodb");
        if (currentFilePath.toFile().isFile()) {
            try {
                return MongoDBConnectionConfiguration.configured(Files.readString(currentFilePath));
            } catch (Throwable ex) {
                return MongoDBConnectionConfiguration.failed(ex);
            }

        }

        return MongoDBConnectionConfiguration.notConfigured();
    }
}