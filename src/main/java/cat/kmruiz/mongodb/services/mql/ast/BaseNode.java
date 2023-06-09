package cat.kmruiz.mongodb.services.mql.ast;

import com.intellij.psi.PsiElement;

import java.util.List;

public abstract class BaseNode implements Node {
    private PsiElement origin;
    private Node parent;
    private List<Node> children;

    public BaseNode(PsiElement origin, List<Node> children) {
        this.origin = origin;
        this.children = children;

        for (var c : children) {
            ((BaseNode) c).setParent(this);
        }
    }

    void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public Node parent() {
        return parent;
    }

    @Override
    public List<Node> children() {
        return children;
    }

    @Override
    public PsiElement origin() {
        return origin;
    }
}
