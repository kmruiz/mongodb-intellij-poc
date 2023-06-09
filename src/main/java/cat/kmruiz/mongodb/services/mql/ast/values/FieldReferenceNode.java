package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.BaseNode;
import com.intellij.psi.PsiElement;

import java.util.Collections;

public final class FieldReferenceNode extends BaseNode {
    private final String name;

    public FieldReferenceNode(PsiElement origin, String name) {
        super(origin, Collections.emptyList());
        this.name = name;
    }

    public String name() {
        return name;
    }
}
