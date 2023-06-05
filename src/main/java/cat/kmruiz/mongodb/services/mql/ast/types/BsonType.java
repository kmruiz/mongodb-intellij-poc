package cat.kmruiz.mongodb.services.mql.ast.types;

import org.bson.types.ObjectId;

import java.util.Date;

public enum BsonType {
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

    BsonType(String visibleName) {
        this.visibleName = visibleName;
    }

    public String toString() {
        return visibleName;
    }

    public static BsonType deduceFrom(Object object) {
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