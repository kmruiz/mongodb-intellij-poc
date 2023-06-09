package cat.kmruiz.mongodb.services.mql.ast.projection;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import com.intellij.psi.PsiElement;

import java.util.List;

public abstract class ProjectionComputationNode extends BaseNode {
    public ProjectionComputationNode(PsiElement origin, List<Node> children) {
        super(origin, children);
    }
}
