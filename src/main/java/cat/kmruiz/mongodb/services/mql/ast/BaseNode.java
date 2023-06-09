package cat.kmruiz.mongodb.services.mql.ast;

import java.util.List;

public abstract class BaseNode<Origin> implements Node<Origin> {
    private Origin origin;
    private Node<Origin> parent;
    private List<Node<Origin>> children;

    public BaseNode(Origin origin, List<Node<Origin>> children) {
        this.origin = origin;
        this.children = children;

        for (var c : children) {
            ((BaseNode<Origin>) c).setParent(this);
        }
    }

    void setParent(Node<Origin> parent) {
        this.parent = parent;
    }

    @Override
    public Node<Origin> parent() {
        return parent;
    }

    @Override
    public List<Node<Origin>> children() {
        return children;
    }

    @Override
    public Origin origin() {
        return origin;
    }
}
