package cat.kmruiz.mongodb.services.mql.ast.varops;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import com.intellij.psi.PsiElement;

import java.util.List;

public final class NorNode extends BaseNode {
      public NorNode(PsiElement origin, List<Node> children) {
        super(origin, children);
    }
}
