package cat.kmruiz.mongodb.services.mql.ast.values;

import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import com.intellij.psi.PsiElement;

import java.util.Collections;
import java.util.Optional;

public final class ConstantValueNode extends ValueNode {
    private final Object constant;

    public ConstantValueNode(PsiElement origin, BsonType type, Object constant) {
        super(origin, Collections.emptyList(), type);
        this.constant = constant;
    }

    @Override
    public Optional<Object> inferValue() {
        return Optional.of(constant);
    }
}
