package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.lang.java.mql.JavaMQLParser;
import cat.kmruiz.mongodb.services.mql.MQLIndexQualityChecker;
import cat.kmruiz.mongodb.services.mql.MQLQueryQualityChecker;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

public class IndexCheckingInspection extends AbstractJavaQueryQualityInspection {
    @Override
    protected MQLQueryQualityChecker qualityChecker(Project project) {
        return project.getService(MQLIndexQualityChecker.class);
    }
}
