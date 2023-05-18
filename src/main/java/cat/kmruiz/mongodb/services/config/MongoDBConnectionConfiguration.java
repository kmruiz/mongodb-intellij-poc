package cat.kmruiz.mongodb.services.config;

public record MongoDBConnectionConfiguration(
        String url,
        Throwable ex
) {
    public static MongoDBConnectionConfiguration configured(String url) {
        return new MongoDBConnectionConfiguration(url, null);
    }

    public static MongoDBConnectionConfiguration notConfigured() {
        return new MongoDBConnectionConfiguration(null, null);
    }

    public static MongoDBConnectionConfiguration failed(Throwable ex) {
        return new MongoDBConnectionConfiguration(null, ex);
    }

    public boolean isConfigured() {
        return url != null;
    }

    public boolean isFailed() {
        return url != null && ex != null;
    }
}
