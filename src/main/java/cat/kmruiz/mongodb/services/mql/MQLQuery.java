package cat.kmruiz.mongodb.services.mql;

import com.intellij.psi.PsiElement;

import java.util.LinkedHashSet;

public record MQLQuery(PsiElement parent, LinkedHashSet<MQLQueryField> fields) {
    public record MQLQueryField(String fieldName, boolean wildcard) {
        public static MQLQueryField newWildcard() {
            return new MQLQueryField(null, true);
        }

        public static MQLQueryField named(String name) {
            return new MQLQueryField(name, false);
        }
    }

    public boolean hasWildcardField() {
        return fields.stream().anyMatch(MQLQueryField::wildcard);
    }

    public boolean hasHighCardinality() {
        return fields.size() > 3;
    }

    public MQLIndex deduceIndex() {
        return new MQLIndex(null,
                fields.stream().map(e -> new MQLIndex.MQLIndexField(e.fieldName, MQLIndex.MQLIndexType.ASC)).toList(),
                false);
    }
}
