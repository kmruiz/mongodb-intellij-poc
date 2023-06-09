package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.Optional;

public abstract class ValueNode extends BaseNode {
    private final BsonType type;

    protected ValueNode(PsiElement origin, List<Node> children, BsonType type) {
        super(origin, children);
        this.type = type;
    }

    public BsonType type() {
        return type;
    }
    public abstract Optional<Object> inferValue();
}
