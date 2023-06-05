package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import cat.kmruiz.mongodb.services.mql.reporting.QueryWarning;
import cat.kmruiz.mongodb.services.schema.CollectionSchema;
import com.intellij.psi.PsiElement;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record MQLQuery<Node>(PsiElement parent, LinkedHashSet<Predicate<Node>> predicates) {
    public record Predicate<Node>(Node fieldNode, Node valueNode, String fieldName, boolean wildcardField, BsonType fieldType, Set<BsonType> types, List<QueryWarning> warnings) {
        public static <Node> Predicate<Node> newWildcard(Node fieldNode, Node valueNode, List<QueryWarning> warning) {
            return new Predicate<>(fieldNode, valueNode, null, true, BsonType.ANY, Collections.emptySet(), warning);
        }

        public static <Node> Predicate<Node> named(Node fieldNode, Node valueNode, String name, BsonType fieldType, Set<BsonType> types, List<QueryWarning> warning) {
            return new Predicate<>(fieldNode, valueNode, name, false, fieldType, types, warning);
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
