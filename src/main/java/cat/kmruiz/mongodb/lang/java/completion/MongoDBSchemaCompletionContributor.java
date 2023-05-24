package cat.kmruiz.mongodb.lang.java.completion;

import cat.kmruiz.mongodb.services.MongoDBFacade;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.Strings;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class MongoDBSchemaCompletionContributor extends CompletionContributor {
    private static final Icon MONGODB_ICON = IconLoader.getIcon("/icons/mongodb-icon.png", MongoDBSchemaCompletionContributor.class);

    public MongoDBSchemaCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(PsiElement.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                        var currentProject = parameters.getEditor().getProject();
                        var mongodbFacade = currentProject.getService(MongoDBFacade.class);
                        var schemaInfo = mongodbFacade.schemaOf("test", "test");

                        if (!schemaInfo.connected()) {
                            return;
                        }

                        var schema = schemaInfo.result();
                        for (var entry : schema.root().entrySet()) {
                            var fieldName = entry.getKey();
                            var value = entry.getValue();

                            var tailText = " types: %s samples: %s".formatted(
                                    Strings.join(value.types(), " | "),
                                    Strings.join(value.samples().stream().limit(3).toList(), "|")
                            );

                            if (value.isIndexed()) {
                                tailText = " [Indexed] " + tailText;
                            }

                            result.addElement(
                                    PrioritizedLookupElement.withPriority(
                                            LookupElementBuilder
                                                    .create(fieldName)
                                                    .withTailText(tailText)
                                                    .withIcon(MONGODB_ICON)
                                                    .withBoldness(value.isIndexed())
                                                    .withItemTextForeground(value.isIndexed() ? JBColor.GREEN : JBColor.GRAY),
                                            value.isIndexed() ? 150 : 50
                                    )
                            );
                        }
                    }
                });
    }
}
