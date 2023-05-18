package cat.kmruiz.mongodb;

import cat.kmruiz.mongodb.services.MongoDBConfigurationResolver;
import cat.kmruiz.mongodb.ui.NotificationSystem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class ProjectListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        var mdbConfigService = project.getService(MongoDBConfigurationResolver.class);
        var mdbConfig = mdbConfigService.getMongoDBConnectionConfiguration();
        var notifications = NotificationSystem.getInstance(project);

        if (!mdbConfig.isConfigured()) {
            if (mdbConfig.isFailed()) {
                notifications.showError("Failed to load configuration in the .mongodb configuration file.", mdbConfig.ex());
            } else {
                notifications.showWarning("<b>MongoDB Plugin Configuration File .mongodb missing.</b><br />It means that some capabilities, like index checking or query running won't be available.");
            }
        }
    }

}
