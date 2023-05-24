package cat.kmruiz.mongodb.services.schema;

import org.bson.Document;

import java.util.*;

public record CollectionSchema(String database, String collection, Map<String, FieldValue> root) {
    public record FieldValue(boolean isIndexed, Set<Type> types, Set<String> samples) {
        enum Type {
            OBJECT_ID,
            STRING,
            INTEGER,
            LONG,
            DOCUMENT,
            ARRAY
        }
    }

    public CollectionSchema merge(Set<String> indexedFields, Document document) {
        var newRoot = new HashMap<>(root);
        for (var field : document.entrySet()) {
            var key = field.getKey();
            var value = field.getValue();

            var existingFieldValue = root.getOrDefault(key, new FieldValue(false, new HashSet<>(), new HashSet<>()));
            var valueType = FieldValue.Type.STRING;

            var newTypes = new HashSet<>(existingFieldValue.types);
            newTypes.add(valueType);

            var newSamples = new HashSet<>(existingFieldValue.samples);
            newSamples.add(value.toString());

            var isIndexed = existingFieldValue.isIndexed || indexedFields.contains(key);

            newRoot.put(key, new FieldValue(isIndexed, newTypes, newSamples));
        }

        return new CollectionSchema(database, collection, newRoot);
    }
}
