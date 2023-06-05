package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;

public interface MQLQueryQualityChecker {
    void check(QueryNode<PsiElement> query, ProblemsHolder holder);
}
