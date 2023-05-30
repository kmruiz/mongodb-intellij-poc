package cat.kmruiz.mongodb.lang.java.completion;

import cat.kmruiz.mongodb.lang.java.perception.MQLQueryPerception;
import cat.kmruiz.mongodb.services.MongoDBFacade;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.Strings;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MongoDBSchemaCompletionContributor extends CompletionContributor {
    private static final Icon MONGODB_ICON = IconLoader.getIcon("/icons/mongodb-icon.png", MongoDBSchemaCompletionContributor.class);
    private static final MQLQueryPerception queryPerception = new MQLQueryPerception();

    public MongoDBSchemaCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(PsiElement.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                        var currentProject = parameters.getEditor().getProject();
                        var mongodbFacade = currentProject.getService(MongoDBFacade.class);
                        var psiElement = parameters.getPosition().getParent();
                        MQLQueryPerception.MQLQueryOrNotPerceived perception = null;

                        var queryCall = inQueryCall(psiElement);
                        if (queryCall == null) {
                            return;
                        }

                        var resolvedMethod = queryCall.resolveMethod();

                        if (resolvedMethod == null) {
                            return;
                        }

                        var owningClass = resolvedMethod.getContainingClass();

                        if (owningClass == null) {
                            return;
                        }

                        if (!owningClass.getQualifiedName().equals("com.mongodb.client.MongoCollection")) {
                            return;
                        }

                        perception = queryPerception.parse(queryCall);

                        if (perception == null) {
                            return;
                        }

                        if (!perception.hasBeenPerceived()) {
                            return;
                        }

                        var schemaInfo = mongodbFacade.schemaOf(perception.database(), perception.collection());

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

    private PsiMethodCallExpression inQueryCall(PsiElement psiElement) {
        var parent = psiElement;
        do {
            var prevParent = parent.getParent();
            System.out.println(prevParent);
            if (prevParent instanceof PsiMethodCallExpression && prevParent.getText().contains(".find")) {
                return (PsiMethodCallExpression) prevParent;
            }

            if (prevParent == null || prevParent == parent) {
                return null;
            }

            parent = prevParent;
        } while (true);
    }
}
