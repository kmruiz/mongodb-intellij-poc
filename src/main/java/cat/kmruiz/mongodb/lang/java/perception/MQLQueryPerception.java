package cat.kmruiz.mongodb.lang.java.perception;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MQLQueryPerception {
    public record MQLQueryField(String fieldName, boolean wildcard) {
        public static MQLQueryField newWildcard() {
            return new MQLQueryField(null, true);
        }

        public static MQLQueryField named(String name) {
            return new MQLQueryField(name, false);
        }
    }
    public record MQLQuery(PsiElement parent, LinkedHashSet<MQLQueryField> fields) {
        public boolean hasWildcardField() {
            return fields.stream().anyMatch(MQLQueryField::wildcard);
        }
    }

    public record MQLQueryOrNotPerceived(String database, String collection, MQLQuery query) {
        public boolean hasBeenPerceived() {
            return query != null;
        }

        public static MQLQueryOrNotPerceived notPerceived() {
            return new MQLQueryOrNotPerceived(null, null,null);
        }
        public static MQLQueryOrNotPerceived perceived(String database, String collection, MQLQuery query) {
            return new MQLQueryOrNotPerceived(database, collection,query);
        }
    }

    public MQLQueryOrNotPerceived parse(@NotNull PsiMethod method) {
        if (!method.getReturnType().equalsToText("org.bson.Document")) {
            return MQLQueryOrNotPerceived.notPerceived();
        }

        var queryDescription = new LinkedHashSet<MQLQueryField>();
        for (var child : method.getBody().getStatements()) {
            if (child instanceof PsiReturnStatement returnStatement) {
                var returnValue = returnStatement.getReturnValue();
                if (returnValue instanceof PsiNewExpression newDoc) {
                    var constructorMethod = newDoc.resolveConstructor();
                    var returnType = constructorMethod.getContainingClass();

                    if (returnType.getQualifiedName().equals("org.bson.Document")) {
                        // we are in "new Document(key, value)"
                        var arguments = newDoc.getArgumentList().getExpressions();
                        var field = arguments[0];

                        if (field instanceof PsiLiteralExpression) {
                            queryDescription.add(MQLQueryField.named(field.getText()));
                        } else {
                            queryDescription.add(MQLQueryField.newWildcard());
                        }
                    }
                }
            }
        }

        return MQLQueryOrNotPerceived.perceived("test", "test", new MQLQuery(method, queryDescription));
    }
}
