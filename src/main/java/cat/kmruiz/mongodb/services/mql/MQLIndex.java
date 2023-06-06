package cat.kmruiz.mongodb.services.mql;

import cat.kmruiz.mongodb.infrastructure.language.ListOperation;
import org.bson.Document;

import java.util.List;
import java.util.Objects;

public record MQLIndex(String indexName, List<MQLIndexField> definition, boolean shardKey, boolean unique) {
    enum MQLIndexType {
        ASC("1"),
        DESC("-1"),
        TEXT("\"text\""),
        SPHERE2D("\"2dsphere\""),
        UNKNOWN("\"?\"");

        private final String serialization;

        MQLIndexType(String serialization) {
            this.serialization = serialization;
        }

        public String toJson() {
            return serialization;
        }
    }

    public record MQLIndexField(String fieldName, MQLIndexType type) {
        public String toJson() {
            return "\"%s\":%s".formatted(fieldName, type.toJson());
        }
    }

    public static MQLIndex parseIndex(Document document) {
        var name = document.getString("name");
        var definition = document.get("key", Document.class);
        var isUnique = Objects.requireNonNullElse(document.get("unique", Boolean.class), false);

        var parsedDef = definition.entrySet().stream().map(kv -> {
            var keyName = kv.getKey();
            var keyValue = kv.getValue().toString();

            return new MQLIndexField(keyName, switch (keyValue) {
                case "1" -> MQLIndexType.ASC;
                case "-1" -> MQLIndexType.DESC;
                default -> MQLIndexType.UNKNOWN;
            });
        }).toList();

        return new MQLIndex(name, parsedDef, false, isUnique);
    }

    public MQLIndex markAsShardingKey() {
        return new MQLIndex(indexName, definition, true, unique);
    }

    public boolean isSameAs(MQLIndex otherIndex) {
        return ListOperation.zip(definition, otherIndex.definition)
                .stream().allMatch(zipped ->
                        zipped.left().fieldName.equals(zipped.right().fieldName) &&
                                zipped.left().type.equals(zipped.right().type)
                );
    }

    public String toJson() {
        return """
                {"name":"%s","key":{%s}, "unique": %s}"""
                .formatted(
                        indexName,
                        String.join(",", definition.stream().map(MQLIndexField::toJson).toList()),
                        unique
                );
    }

    public String toCreationJson() {
        return "{%s}"
                .formatted(
                        String.join(",", definition.stream().map(MQLIndexField::toJson).toList())
                );
    }
}
