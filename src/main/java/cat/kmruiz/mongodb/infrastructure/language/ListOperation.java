package cat.kmruiz.mongodb.infrastructure.language;

import org.apache.tools.ant.taskdefs.Zip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ListOperation {
    private ListOperation() {}

    public record Zipped<L, R>(L left, R right) {}

    public static <L, R> List<Zipped<L, R>> zip(List<L> l, List<R> r) {
        Iterator<L> it1 = l.iterator();
        Iterator<R> it2 = r.iterator();

        var result = new ArrayList<Zipped<L, R>>(Math.max(l.size(), r.size()));
        while (it1.hasNext() || it2.hasNext()) {
            var left = it1.hasNext() ? it1.next() : null;
            var right = it2.hasNext() ? it2.next() : null;

            result.add(new Zipped<>(left, right));
        }

        return result;
    }

}
