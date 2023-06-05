package cat.kmruiz.mongodb.services.mql.ast;

import java.util.Collections;
import java.util.List;

public record InvalidMQLNode<Origin>(Origin origin, Origin collectionReference, Reason reason) implements Node<Origin> {
    public enum Reason {
        UNKNOWN_NAMESPACE, INVALID_QUERY
    }

    @Override
    public List<Node<Origin>> children() {
        return Collections.emptyList();
    }
}
