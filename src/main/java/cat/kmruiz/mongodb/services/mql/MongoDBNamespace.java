package cat.kmruiz.mongodb.services.mql;

public record MongoDBNamespace(String database, String collection) {
    public boolean isKnown() {
        return database != null && collection != null;
    }

    public static MongoDBNamespace defaultTest() {
        return new MongoDBNamespace("test", "test");
    }

    public static MongoDBNamespace fromQualifiedNamespace(String namespace) {
        var nsSplit = namespace.split("\\.");
        return new MongoDBNamespace(nsSplit[0], nsSplit[1]);
    }
}
