
package cat.kmruiz.mongodb.ui;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class QuickFixBundle extends DynamicBundle {
    private static final QuickFixBundle ourInstance = new QuickFixBundle();

    @NonNls
    public static final String BUNDLE = "messages.QuickfixBundle";

    private QuickFixBundle() {
        super(BUNDLE);
    }

    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                      Object @NotNull ... params) {
        return ourInstance.getMessage(key, params);
    }
}
