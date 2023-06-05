package cat.kmruiz.mongodb.services.mql.ast;

import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;

import java.util.Collections;
import java.util.List;

public record PredicateNode<Origin, Type>(Origin origin, String field, ValueNode<Origin, Type> value) implements Node<Origin> {
    @Override
    public List<Node<Origin>> children() {
        return Collections.singletonList(value);
    }
}
