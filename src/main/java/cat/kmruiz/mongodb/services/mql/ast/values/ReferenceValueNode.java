package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import com.intellij.psi.PsiElement;

import java.util.Collections;
import java.util.Optional;

public final class ReferenceValueNode extends ValueNode {
    public ReferenceValueNode(PsiElement origin, BsonType type) {
        super(origin, Collections.emptyList(), type);
    }

    @Override
    public Optional<Object> inferValue() {
        return Optional.empty();
    }
}
