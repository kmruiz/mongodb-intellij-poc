package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.lang.java.mql.JavaMQLParser;
import cat.kmruiz.mongodb.lang.java.perception.MQLQueryPerception;
import cat.kmruiz.mongodb.lang.java.quickfix.AddJavaDocForNamespace;
import cat.kmruiz.mongodb.lang.java.quickfix.DeduceIndexQuickFix;
import cat.kmruiz.mongodb.lang.java.quickfix.RunQueryOnSecondaryNode;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MQLQueryQualityChecker;
import cat.kmruiz.mongodb.services.mql.MQLTypeChecker;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.ui.IndexBeautifier;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

public class TypeCheckingInspection extends AbstractJavaQueryQualityInspection {
    @Override
    protected MQLQueryQualityChecker qualityChecker(Project project) {
        return project.getService(MQLTypeChecker.class);
    }
}
