package cat.kmruiz.mongodb.services.mql.ast.aggregate;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import com.intellij.psi.PsiElement;

import java.util.List;

public final class AggregateMatchStageNode extends BaseNode {
    public AggregateMatchStageNode(PsiElement origin, List<Node> children) {
        super(origin, children);
    }
}
