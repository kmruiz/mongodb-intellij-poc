package cat.kmruiz.mongodb.ui;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class NotificationSystem {
    private final Project project;

    private NotificationSystem(Project project) {
        this.project = project;
    }

    public static NotificationSystem getInstance(@NotNull Project project) {
        return new NotificationSystem(project);
    }
    public void showError(String error, Throwable ex) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("MongoDB Plugin")
                .createNotification("<b>" + error + "</b><br /><br />" + ex.getMessage(), NotificationType.ERROR)
                .notify(project);
    }

    public void showInfo(String info) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("MongoDB Plugin")
                .createNotification(info, NotificationType.INFORMATION)
                .notify(project);
    }

    public void showWarning(String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("MongoDB Plugin")
                .createNotification(message, NotificationType.WARNING)
                .notify(project);
    }
}
