package cat.kmruiz.mongodb.lang.java.inlay;

import cat.kmruiz.mongodb.lang.java.QueryIndexingQualityInspection;
import cat.kmruiz.mongodb.lang.java.completion.MongoDBSchemaCompletionContributor;
import cat.kmruiz.mongodb.lang.java.perception.MQLQueryPerception;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import com.intellij.codeInsight.hints.*;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class IndexUsageInlay implements InlayHintsProvider<NoSettings> {
    private static final Icon MONGODB_ICON = IconLoader.getIcon("/icons/mongodb-icon-small.png", MongoDBSchemaCompletionContributor.class);

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
        return new IndexUsageInlayCollector(editor);
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return language.is(Language.findLanguageByID("JAVA"));
    }

    public static class IndexUsageInlayCollector extends FactoryInlayHintsCollector {
        private final MQLQueryPerception queryPerception = new MQLQueryPerception();
        private final MongoDBFacade facade;

        public IndexUsageInlayCollector(@NotNull Editor editor) {
            super(editor);

            this.facade = editor.getProject().getService(MongoDBFacade.class);
        }

        @Override
        public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
            MQLQueryPerception.MQLQueryOrNotPerceived perception = null;

            if ((psiElement instanceof PsiMethodCallExpression methodCall)) {
                var resolvedMethod = methodCall.resolveMethod();

                if (resolvedMethod == null) {
                    return true;
                }

                var owningClass = resolvedMethod.getContainingClass();

                if (owningClass == null) {
                    return true;
                }

                if (owningClass.getQualifiedName().equals("com.mongodb.client.MongoCollection")) {
                    perception = queryPerception.parse(methodCall);
                } else {
                    return true;
                }
            } else {
                return true;
            }

            if (!perception.hasBeenPerceived()) {
                return true;
            }

            var query = perception.query();
            var candidateIndexes = facade.candidateIndexesForQuery(perception.database(), perception.collection(), query).result();

            if (!candidateIndexes.isEmpty()) {
                var index = candidateIndexes.get(0);

                var icon = getFactory().smallScaledIcon(MONGODB_ICON);
                var text = getFactory().smallText((index.shardKey() ? "[Sharding Key] " : "") + index.toJson());
                text = getFactory().roundWithBackground(text);

                var representation = getFactory().seq(icon, text);
                inlayHintsSink.addInlineElement(psiElement.getTextOffset(), true, representation, true);
            }

            if (perception.collectionDeclaration() != null) {
                var icon = getFactory().smallScaledIcon(MONGODB_ICON);
                var text = getFactory().smallText(perception.database() + "." + perception.collection());
                text = getFactory().roundWithBackground(text);
                var representation = getFactory().seq(icon, text);

                inlayHintsSink.addInlineElement(psiElement.getParent().getParent().getTextOffset(), false, representation, true);
            }

            return true;
        }
    }
}
