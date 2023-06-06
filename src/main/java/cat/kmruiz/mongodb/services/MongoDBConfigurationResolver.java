package cat.kmruiz.mongodb.services;

import cat.kmruiz.mongodb.services.config.MongoDBConnectionConfiguration;
import cat.kmruiz.mongodb.ui.NotificationSystem;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.psi.DataSourceManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class MongoDBConfigurationResolver {
    private final Project currentProject;
    private final List<String> connectedDatasources;

    public MongoDBConfigurationResolver(Project project) {
        this.currentProject = project;
        this.connectedDatasources = new ArrayList<>();
    }

    public MongoDBConnectionConfiguration getMongoDBConnectionConfiguration() {
        var dataSourceManagers = DataSourceManager.getManagers(currentProject);
        var dataSources = new ArrayList<LocalDataSource>();

        for (var dm : dataSourceManagers) {
            for (var ds : dm.getDataSources()) {
                if (ds.getDbms().isMongo()) {
                    dataSources.add((LocalDataSource) ds);
                }
            }
        }

        if (dataSources.isEmpty()) {
            return MongoDBConnectionConfiguration.notConfigured();
        }

        if (dataSources.size() == 1) {
            return forDataSource(dataSources.get(0));
        }

        LocalDataSource byReadonly = null, byComment = null;

        // try to deduce what is the best DS
        for (var ds : dataSources) {
            if (ds.isReadOnly()) {
                byReadonly = ds;
            }

            if (ds.getComment() != null && ds.getComment().startsWith("[MongoDB Plugin]")) {
                byComment = ds;
            }
        }

        if (byComment != null) {
            return forDataSource(byComment);
        }

        if (byReadonly != null) {
            return forDataSource(byReadonly);
        }

        NotificationSystem.getInstance(currentProject)
                .showWarning("[MongoDB Plugin] Could not connect to a MongoDB datasource because multiple are configured.");

        return MongoDBConnectionConfiguration.notConfigured();
    }

    private MongoDBConnectionConfiguration forDataSource(@NotNull LocalDataSource dataSource) {
        var connectionConfig = dataSource.getConnectionConfig();
        var dataSourceName = connectionConfig.getName();

        if (!connectedDatasources.contains(dataSourceName)) {
            NotificationSystem.getInstance(currentProject).showInfo("[MongoDB Plugin] Connected to " + dataSourceName);
            connectedDatasources.add(dataSourceName);
        }

        return MongoDBConnectionConfiguration.configured(connectionConfig.getUrl(), dataSource);
    }
}