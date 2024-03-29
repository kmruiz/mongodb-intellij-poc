package cat.kmruiz.mongodb.lang.java.inlay;

import cat.kmruiz.mongodb.infrastructure.PsiMongoDBTreeUtils;
import cat.kmruiz.mongodb.lang.java.completion.MongoDBSchemaCompletionContributor;
import cat.kmruiz.mongodb.lang.java.mql.JavaMongoDBDriverMQLParser;
import cat.kmruiz.mongodb.services.mql.MQLQueryIndexAnalyzer;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import com.intellij.codeInsight.hints.*;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class IndexUsageInlay implements InlayHintsProvider<NoSettings> {
    private static final Icon MONGODB_ICON = IconLoader.getIcon("/icons/mongodb-icon.png", MongoDBSchemaCompletionContributor.class);

    @Override
    public boolean isVisibleInSettings() {
        return false;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return new SettingsKey<>("mongodb.IndexUsageInlay");
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "Index usage";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Index usage";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings noSettings) {
        return changeListener -> new JPanel();
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull NoSettings noSettings, @NotNull InlayHintsSink inlayHintsSink) {
        return new FieldTypeInlayCollector(editor);
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return language.is(Language.findLanguageByID("JAVA"));
    }

    public static class FieldTypeInlayCollector extends FactoryInlayHintsCollector {
        private final JavaMongoDBDriverMQLParser parser;
        private final MQLQueryIndexAnalyzer indexAnalyzer;
        private final Map<Integer, String> addedInlays;

        public FieldTypeInlayCollector(@NotNull Editor editor) {
            super(editor);

            this.parser = editor.getProject().getService(JavaMongoDBDriverMQLParser.class);
            this.indexAnalyzer = editor.getProject().getService(MQLQueryIndexAnalyzer.class);

            this.addedInlays = new HashMap<>();
        }

        @Override
        public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
            var currentMethodExpression = PsiMongoDBTreeUtils.asMongoDBExpression(psiElement);

            if (currentMethodExpression == null) {
                return true;
            }

            var parsedQuery = parser.parse(currentMethodExpression);
            if (parsedQuery instanceof QueryNode query) {
                var indexResult = this.indexAnalyzer.indexesOfQuery(query);
                if (indexResult.isEmpty()) {
                    return true;
                }

                var indexHash = indexResult.get(0).toJson();
                if (!this.addedInlays.getOrDefault(currentMethodExpression.getTextOffset(), "").equals(indexHash)) {
                    var presentation = getFactory().seq(
                            getFactory().smallScaledIcon(MONGODB_ICON),
                            getFactory().text(indexHash)
                    );

                    inlayHintsSink.addInlineElement(currentMethodExpression.getTextOffset() + currentMethodExpression.getTextLength(), true, presentation, false);
                    this.addedInlays.put(currentMethodExpression.getTextOffset(), indexHash);
                }

            }

            return true;
        }
    }
}
