package cat.kmruiz.mongodb.services.config;

import com.intellij.database.dataSource.LocalDataSource;

public record MongoDBConnectionConfiguration(
        String url,
        LocalDataSource dataSource,
        Throwable ex
) {
    public static MongoDBConnectionConfiguration configured(String url, LocalDataSource dbElement) {
        return new MongoDBConnectionConfiguration(url, dbElement, null);
    }

    public static MongoDBConnectionConfiguration notConfigured() {
        return new MongoDBConnectionConfiguration(null, null, null);
    }

    public static MongoDBConnectionConfiguration failed(Throwable ex) {
        return new MongoDBConnectionConfiguration(null, null, ex);
    }

    public boolean isConfigured() {
        return url != null;
    }

    public boolean isFailed() {
        return url != null && ex != null;
    }
}
