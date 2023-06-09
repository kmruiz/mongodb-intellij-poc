package cat.kmruiz.mongodb.services.mql.ast;

import java.util.List;

public interface Node<Origin> {
    Node<Origin> parent();
    Origin origin();
    List<Node<Origin>> children();
}
