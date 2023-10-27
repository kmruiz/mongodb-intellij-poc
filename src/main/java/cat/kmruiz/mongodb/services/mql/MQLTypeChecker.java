package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import cat.kmruiz.mongodb.services.mql.ast.values.ValueNode;
import cat.kmruiz.mongodb.services.schema.CollectionSchema;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.PsiElement;

@Service(Service.Level.PROJECT)
public final class MQLTypeChecker implements MQLQueryQualityChecker {
    private final MongoDBFacade mongoDBFacade;

    public MQLTypeChecker(Project project) {
        this.mongoDBFacade = project.getService(MongoDBFacade.class);
    }

    public void check(QueryNode query, ProblemsHolder holder) {
        var schema = mongoDBFacade.schemaOf(query.namespace());
        if (!schema.connected()) {
            return;
        }

        typeCheckRecursively(query, schema.result(), holder);
    }

    private void typeCheckRecursively(Node node, CollectionSchema schema, ProblemsHolder holder) {
        if (node instanceof BinOpNode binOp) {
            for (var cond : binOp.children()) {
                if (cond instanceof ValueNode refVal) {
                    var fieldSchema = schema.ofField(binOp.field().name());
                    if (!fieldSchema.supportsProvidedType(refVal.type())) {
                        holder.registerProblem(
                                refVal.origin(),
                                InspectionBundle.message("inspection.MQLQueryPerception.warning.fieldTypeDoesNotMatch",
                                        binOp.field().name(), refVal.type(), Strings.join(fieldSchema.types(), "|")
                                )
                        );
                    }
                }
            }
        } else {
            node.children().forEach(child -> typeCheckRecursively(child, schema, holder));
        }
    }
}
