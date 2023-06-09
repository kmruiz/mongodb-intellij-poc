package cat.kmruiz.mongodb.services.mql.ast;

import com.intellij.psi.PsiElement;

import java.util.Collections;
import java.util.List;

public record InvalidMQLNode(PsiElement origin, PsiElement collectionReference, Reason reason) implements Node {
    @Override
    public Node parent() {
        return null;
    }

    public enum Reason {
        UNKNOWN_NAMESPACE, INVALID_QUERY
    }

    @Override
    public List<Node> children() {
        return Collections.emptyList();
    }
}
