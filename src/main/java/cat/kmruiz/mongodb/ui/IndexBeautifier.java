package cat.kmruiz.mongodb.ui;

import cat.kmruiz.mongodb.services.mql.MQLIndex;
import org.bson.Document;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class IndexBeautifier {
    public static String beautify(List<MQLIndex> indexDef) {
        return String.join("\n\t", indexDef.stream().sorted(new SortByCardinality()).map(MQLIndex::toJson).toList());
    }

    public static String beautify(MQLIndex indexDef) {
        return indexDef.toJson();
    }

    private static class SortByCardinality implements Comparator<MQLIndex> {
        @Override
        public int compare(MQLIndex o1, MQLIndex o2) {
            var leftCardinality = o1.definition().size();
            var rightCardinality = o2.definition().size();

            return Integer.compare(leftCardinality, rightCardinality);
        }
    }
}
