package cat.kmruiz.mongodb.lang.java;

import cat.kmruiz.mongodb.lang.java.perception.MQLQueryPerception;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.ui.IndexBeautifier;
import cat.kmruiz.mongodb.ui.InspectionBundle;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public class QueryIndexingQualityInspection extends AbstractBaseJavaLocalInspectionTool {
    private final MQLQueryPerception queryPerception = new MQLQueryPerception();

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        var facade = holder.getProject().getService(MongoDBFacade.class);

        return new JavaElementVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                var perception = queryPerception.parse(method);
                if (!perception.hasBeenPerceived()) {
                    return;
                }

                var indexes = facade.indexesOfCollection(perception.database(), perception.collection());
                if (!indexes.connected()) {
                    return;
                }

                holder.registerProblem(method,
                        InspectionBundle.message("inspection.QueryIndexingQualityInspection.messageTemplate",
                                perception.database(),
                                perception.collection(),
                                IndexBeautifier.beautify(indexes.result())));
            }
        };
    }
}
