package cat.kmruiz.mongodb.lang.java.inlay;

import cat.kmruiz.mongodb.infrastructure.PsiMongoDBTreeUtils;
import cat.kmruiz.mongodb.lang.java.mql.JavaMQLParser;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.ast.Node;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.binops.BinOpNode;
import com.intellij.codeInsight.hints.*;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.Strings;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

public class QueryFieldTypeInferenceInlay implements InlayHintsProvider<NoSettings> {
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
        private final JavaMQLParser parser;
        private final MongoDBFacade facade;

        public FieldTypeInlayCollector(@NotNull Editor editor) {
            super(editor);

            this.facade = editor.getProject().getService(MongoDBFacade.class);
            this.parser = editor.getProject().getService(JavaMQLParser.class);
        }

        @Override
        public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
            var currentMethodExpression = PsiMongoDBTreeUtils.asMongoDBExpression(psiElement);
            if (currentMethodExpression == null) {
                return true;
            }

            var parsedQuery = parser.parse(currentMethodExpression);
            if (parsedQuery instanceof QueryNode<PsiElement> query) {
                var schemaResult = this.facade.schemaOf(query.namespace());
                if (!schemaResult.connected()) {
                    return true;
                }

                var schema = schemaResult.result();

                iterateOverAllFieldReferences(query, binOp -> {
                    var typeOfField = schema.ofField(binOp.field());
                    var fieldTypes = getFactory().text(": " + Strings.join(typeOfField.types(), " | "));

                    inlayHintsSink.addInlineElement(binOp.fieldOrigin().getTextOffset() + binOp.fieldOrigin().getTextLength(), true, fieldTypes, false);
                });

                return false;
            }

            return true;
        }

        private void iterateOverAllFieldReferences(Node<PsiElement> root, Consumer<BinOpNode<PsiElement>> fn) {
            if (root instanceof BinOpNode<PsiElement> binOp) {
                fn.accept(binOp);
            }

            for (var child : root.children()) {
                iterateOverAllFieldReferences(child, fn);
            }
        }
    }
}
