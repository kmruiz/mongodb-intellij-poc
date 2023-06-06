package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.services.mql.MQLQueryQualityChecker;
import cat.kmruiz.mongodb.services.mql.MQLTypeChecker;
import com.intellij.openapi.project.Project;

public class TypeCheckingInspection extends AbstractJavaQueryQualityInspection {
    @Override
    protected MQLQueryQualityChecker qualityChecker(Project project) {
        return project.getService(MQLTypeChecker.class);
    }
}
