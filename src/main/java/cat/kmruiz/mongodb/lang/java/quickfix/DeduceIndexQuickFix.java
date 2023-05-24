package cat.kmruiz.mongodb.lang.java.quickfix;

import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MQLQuery;
import cat.kmruiz.mongodb.ui.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class DeduceIndexQuickFix implements LocalQuickFix {
    private final String database;
    private final String collection;
    private final MQLQuery failingQuery;

    public DeduceIndexQuickFix(String database, String collection, MQLQuery failingQuery) {
        this.database = database;
        this.collection = collection;
        this.failingQuery = failingQuery;
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
        var indexScript = "use %s;\ndb.%s.createIndex(%s);".formatted(database, collection, failingQuery.deduceIndex().toCreationJson());
        var stringSelection = new StringSelection(indexScript);
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
