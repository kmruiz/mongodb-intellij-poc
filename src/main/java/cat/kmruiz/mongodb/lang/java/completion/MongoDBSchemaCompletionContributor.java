package cat.kmruiz.mongodb.lang.java.completion;

import cat.kmruiz.mongodb.infrastructure.PsiMongoDBTreeUtils;
import cat.kmruiz.mongodb.lang.java.mql.JavaMongoDBDriverMQLParser;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.Strings;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MongoDBSchemaCompletionContributor extends CompletionContributor {
    private static final Icon MONGODB_ICON = IconLoader.getIcon("/icons/mongodb-icon.png", MongoDBSchemaCompletionContributor.class);

    public MongoDBSchemaCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(PsiElement.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                        var currentProject = parameters.getEditor().getProject();
                        var parser = currentProject.getService(JavaMongoDBDriverMQLParser.class);
                        var mongodbFacade = currentProject.getService(MongoDBFacade.class);

                        var currentMethodExpression = PsiMongoDBTreeUtils.getMongoDBQueryExpression(parameters.getPosition());
                        if (currentMethodExpression == null) {
                            return;
                        }

                        var parsedQuery = parser.parse(currentMethodExpression);
                        if (parsedQuery instanceof QueryNode<PsiElement> query) {
                            var schemaInfo = mongodbFacade.schemaOf(query.namespace());

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
                    }
                });
    }

}
