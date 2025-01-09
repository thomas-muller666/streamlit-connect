package io.streamlitconnect;

import static org.apache.commons.lang3.Validate.isTrue;

import io.streamlitconnect.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Represents the Streamlit navigation menu in the sidebar. See https://docs.streamlit.io/develop/api-reference/navigation
 * <p>
 * The navigation menu is a list of menu items, each containing a list of navigation entries. If the navigation menu contains only
 * a single menu item with a NULL header, the menu won't have sub-menu headers with entries, but just a flat list of entries.
 * Example:
 * <pre><code>
 *     NavigationMenu menu = new NavigationMenu();
 *     // Add a single menu item with a NULL header
 *     menu.addItems(new NavigationMenu.MenuItem(
 *          null,
 *          List.of(
 *              new NavigationMenu.NavigationEntry("home", "Home", null, true),
 *              new NavigationMenu.NavigationEntry("about", "About", null, false)
 *              )
 *           ));
 *  </pre></code>
 * The above example will render a flat list of entries in the sidebar menu.
 * <p>
 * See <a href="https://docs.streamlit.io/develop/api-reference/navigation/st.navigation">st.navigation</a>
 */
public class NavigationMenu {

    public enum Location {
        SIDEBAR,
        HIDDEN
    }

    public record MenuItem(String header, @NonNull List<NavigationEntry> entries) {

        public MenuItem {
            isTrue(!entries.isEmpty(), "Entries cannot be empty");
            isTrue(entries.stream().noneMatch(Objects::isNull), "Entries cannot contain null values");
        }
    }


    /**
     * Represents a navigation entry in the sidebar menu
     *
     * @param pageName  The name of the page to be associated in {@link MultiPageApp#getPage(String)}
     * @param title     The title of the page top be displayed in the sidebar menu
     * @param icon
     * @param isDefault
     */
    public record NavigationEntry(
        @NonNull String pageName, // The name of the page to be associated in Streamlit
        @NonNull String title, // The title of the page top be displayed in the sidebar menu
        String icon,
        boolean isDefault) {

    }

    private final List<MenuItem> items = new ArrayList<>();

    @Getter
    @Setter
    @NonNull
    private Location location = Location.SIDEBAR;

    public NavigationMenu() {
    }

    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItems(@NonNull MenuItem... items) {
        // Check that only one item is default, also include this.items
        long defaultCount = 0;

        for (MenuItem item : items) {
            if (item.entries().stream().anyMatch(NavigationEntry::isDefault)) {
                defaultCount++;
            }
        }

        for (MenuItem item : this.items) {
            if (item.entries().stream().anyMatch(NavigationEntry::isDefault)) {
                defaultCount++;
            }
        }

        isTrue(defaultCount <= 1, "Only one navigation entry can be default");

        Collections.addAll(this.items, items);
    }

    public void removeItem(@NonNull MenuItem item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("location", location)
            .append("items\n", StringUtils.prettyPrint(items))
            .toString();
    }

}
