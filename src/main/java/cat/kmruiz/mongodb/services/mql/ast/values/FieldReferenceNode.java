package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.Node;

import java.util.List;

public record FieldReferenceNode<Origin>(Origin origin, String name) implements Node<Origin> {
    public boolean isKnown() {
        return name != null;
    }

    @Override
    public List<Node<Origin>> children() {
        return null;
    }
}
