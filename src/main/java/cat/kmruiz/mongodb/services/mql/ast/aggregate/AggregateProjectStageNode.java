package cat.kmruiz.mongodb.services.mql.ast.aggregate;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import com.intellij.psi.PsiElement;

import java.util.List;

public final class AggregateProjectStageNode extends BaseNode {
    public AggregateProjectStageNode(PsiElement origin, List<Node> children) {
        super(origin, children);
    }
}
