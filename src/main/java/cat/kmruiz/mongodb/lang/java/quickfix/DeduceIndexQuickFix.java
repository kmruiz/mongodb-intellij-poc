package cat.kmruiz.mongodb.lang.java.quickfix;

import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import cat.kmruiz.mongodb.ui.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DeduceIndexQuickFix implements LocalQuickFix {
    private final MongoDBFacade facade;
    private final MongoDBNamespace namespace;
    private final MQLIndex suggestedIndex;

    public DeduceIndexQuickFix(MongoDBFacade facade, MongoDBNamespace namespace, MQLIndex suggestedIndex) {
        this.namespace = namespace;
        this.suggestedIndex = suggestedIndex;
        this.facade = facade;
    }

    @NotNull
    @Override
    public String getName() {
        return QuickFixBundle.message("quickfix.DeduceIndexQuickFix.name");
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        var indexScript = "use %s;\ndb.%s.createIndex(%s);".formatted(namespace.database(), namespace.collection(), suggestedIndex.toCreationJson());
        this.facade.openDatabaseEditorAppendingCode(indexScript);
    }
}
