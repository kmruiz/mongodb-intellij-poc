package cat.kmruiz.mongodb.services.schema;

import cat.kmruiz.mongodb.infrastructure.PsiMongoDBTreeUtils;
import cat.kmruiz.mongodb.services.mql.ast.QueryNode;
import cat.kmruiz.mongodb.services.mql.ast.types.BsonType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public record CollectionSchema(String database, String collection, Map<String, FieldValue> root) {
    public record FieldValue(boolean isIndexed, Set<BsonType> types, Set<String> samples) {
        public boolean supportsProvidedType(BsonType type) {
            return this.types.contains(type) || type == BsonType.ANY || this.types.contains(BsonType.ANY);
        }
    }

    public FieldValue ofField(String fieldName) {
        return root.getOrDefault(fieldName, new FieldValue(false, Set.of(BsonType.ANY), Collections.emptySet()));
    }

    public CollectionSchema merge(Set<String> indexedFields, Document document) {
        var newRoot = new HashMap<>(root);
        for (var field : document.entrySet()) {
            var key = field.getKey();
            var value = field.getValue();

            var existingFieldValue = root.getOrDefault(key, new FieldValue(false, new HashSet<>(), new HashSet<>()));
            var valueType = BsonType.deduceFrom(value);

            var newTypes = new HashSet<>(existingFieldValue.types);
            newTypes.add(valueType);

            var newSamples = new HashSet<>(existingFieldValue.samples);
            newSamples.add(value.toString());

            var isIndexed = existingFieldValue.isIndexed || indexedFields.contains(key);

            newRoot.put(key, new FieldValue(isIndexed, newTypes, newSamples));
        }

        return new CollectionSchema(database, collection, newRoot);
    }

    public CollectionSchema downTo(QueryNode query, PsiElement caret) {
        var result = PsiMongoDBTreeUtils.findNodeParentOf(query.children(), caret);
        System.out.println(result);
        return this;
    }
}
