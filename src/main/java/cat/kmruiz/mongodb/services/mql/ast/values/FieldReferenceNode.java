package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;

import java.util.Collections;

public final class FieldReferenceNode<Origin> extends BaseNode<Origin> {
    private final String name;

    public FieldReferenceNode(Origin origin, String name) {
        super(origin, Collections.emptyList());
        this.name = name;
    }

    public String name() {
        return name;
    }
}
