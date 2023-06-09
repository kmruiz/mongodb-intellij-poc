package cat.kmruiz.mongodb.services.mql.ast;

import com.intellij.psi.PsiElement;

import java.util.List;

public interface Node {
    Node parent();
    PsiElement origin();
    List<Node> children();
}
