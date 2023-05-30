package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.services.mql.reporting.QueryWarning;
import cat.kmruiz.mongodb.services.schema.CollectionSchema;
import com.intellij.psi.PsiElement;

import java.util.LinkedHashSet;
import java.util.List;

public record MQLQuery<Node>(PsiElement parent, LinkedHashSet<Predicate<Node>> predicates) {
    public record Predicate<Node>(Node fieldNode, Node valueNode, String fieldName, boolean wildcardField, CollectionSchema.FieldValue.Type fieldType, List<QueryWarning> warnings) {
        public static <Node> Predicate<Node> newWildcard(Node fieldNode, Node valueNode, List<QueryWarning> warning) {
            return new Predicate<>(fieldNode, valueNode, null, true, CollectionSchema.FieldValue.Type.ANY, warning);
        }

        public static <Node> Predicate<Node> named(Node fieldNode, Node valueNode, String name, CollectionSchema.FieldValue.Type fieldType, List<QueryWarning> warning) {
            return new Predicate<>(fieldNode, valueNode, name, false, fieldType, warning);
        }
    }

    public boolean hasWildcardField() {
        return predicates.stream().anyMatch(Predicate::wildcardField);
    }

    public boolean hasHighCardinality() {
        return predicates.size() > 3;
    }

    public MQLIndex deduceIndex() {
        return new MQLIndex(null,
                predicates.stream().map(e -> new MQLIndex.MQLIndexField(e.fieldName, MQLIndex.MQLIndexType.ASC)).toList(),
                false);
    }
}
