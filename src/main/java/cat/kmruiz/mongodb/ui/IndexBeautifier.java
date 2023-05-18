package cat.kmruiz.mongodb.ui;

import org.bson.Document;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class IndexBeautifier {
    public static String beautify(List<Document> indexDef) {
        return String.join("\n\t", indexDef.stream().sorted(new SortByCardinality()).map(Document::toJson).toList());
    }

    private static class SortByCardinality implements Comparator<Document> {
        @Override
        public int compare(Document o1, Document o2) {
            var leftCardinality = o1.get("key", Document.class).size();
            var rightCardinality = o2.get("key", Document.class).size();

            return Integer.compare(leftCardinality, rightCardinality);
        }
    }
}
