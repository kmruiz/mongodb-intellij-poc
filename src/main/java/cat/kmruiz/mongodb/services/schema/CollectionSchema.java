package cat.kmruiz.mongodb.services.schema;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

public record CollectionSchema(String database, String collection, Map<String, FieldValue> root) {
    public record FieldValue(boolean isIndexed, Set<Type> types, Set<String> samples) {
        public enum Type {
            OBJECT_ID("objectId"),
            STRING("string"),
            BOOLEAN("bool"),
            INTEGER("int"),
            LONG("long"),
            DECIMAL("decimal"),
            DOUBLE("double"),
            OBJECT("object"),
            DATETIME("date"),
            ARRAY("array"),
            ANY("any");

            private final String visibleName;

            Type(String visibleName) {
                this.visibleName = visibleName;
            }

            public String toString() {
                return visibleName;
            }

            public static Type deduceFrom(Object object) {
                if (object instanceof ObjectId) {
                    return OBJECT_ID;
                }

                if (object instanceof String) {
                    return STRING;
                }

                if (object instanceof Integer) {
                    return INTEGER;
                }

                if (object instanceof Long) {
                    return LONG;
                }

                if (object instanceof Double) {
                    return DOUBLE;
                }

                if (object instanceof Date) {
                    return DATETIME;
                }

                if (object instanceof Boolean) {
                    return BOOLEAN;
                }

                return OBJECT;
            }
        }

        public boolean supportsProvidedType(Type type) {
            return this.types.contains(type);
        }
    }

    public FieldValue ofField(String fieldName) {
        return root.getOrDefault(fieldName, new FieldValue(false, Set.of(FieldValue.Type.ANY), Collections.emptySet()));
    }

    public CollectionSchema merge(Set<String> indexedFields, Document document) {
        var newRoot = new HashMap<>(root);
        for (var field : document.entrySet()) {
            var key = field.getKey();
            var value = field.getValue();

            var existingFieldValue = root.getOrDefault(key, new FieldValue(false, new HashSet<>(), new HashSet<>()));
            var valueType = FieldValue.Type.deduceFrom(value);

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
