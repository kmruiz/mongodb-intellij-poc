package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.services.mql.MQLIndexQualityChecker;
import cat.kmruiz.mongodb.services.mql.MQLQueryQualityChecker;
import com.intellij.openapi.project.Project;

public class IndexCheckingInspection extends AbstractJavaQueryQualityInspection {
    @Override
    protected MQLQueryQualityChecker qualityChecker(Project project) {
        return project.getService(MQLIndexQualityChecker.class);
    }
}
