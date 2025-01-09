package test.streamlitconnect;

import io.streamlitconnect.Container;
import io.streamlitconnect.MultiPageApp;
import io.streamlitconnect.NavigationMenu;
import io.streamlitconnect.NavigationMenu.MenuItem;
import io.streamlitconnect.NavigationMenu.NavigationEntry;
import io.streamlitconnect.NavigationRequestContext;
import io.streamlitconnect.OperationsRequestContext;
import io.streamlitconnect.Page;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.LinkButton;
import io.streamlitconnect.widgets.PageLink;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamlitMultiPageTest {

    private static final Logger log = LoggerFactory.getLogger(StreamlitMultiPageTest.class);

    private static class TestMultiPageApp implements MultiPageApp {

        private final LinkButton vgLink = new LinkButton("VG", "https://www.vg.no");

        private final Page page1 = new Page() {

            private final Button button = new Button("Click me");

            @Override
            public String getName() {
                return "page_1";
            }

            @Override
            public void render(OperationsRequestContext context) {
                Container root = context.getRootContainer();
                root.title("This is page 1");
                root.widget(new PageLink("page_2", "Go to page 2"));
                root.widget(vgLink);
                root.widget(button);
                if (button.isChanged()) {
                    root.text("Button was clicked");
                }
            }

        };

        private final Page page2 = new Page() {

            @Override
            public String getName() {
                return "page_2";
            }

            @Override
            public void render(OperationsRequestContext context) {
                Container root = context.getRootContainer();
                root.title("This is page 2");
                root.widget(new PageLink("page_1", "Go to page 1"));
            }

        };

        @Override
        public @NotNull Page getPage(@NonNull String name) {
            log.debug("getPage: {}", name);

            return switch (name) {
                case "page_1" -> page1;
                case "page_2" -> page2;
                default -> throw new IllegalArgumentException("Unknown page: " + name);
            };

        }

        @Override
        public NavigationMenu getNavigationMenu(NavigationRequestContext context) {
            NavigationMenu menu = new NavigationMenu();
            NavigationEntry entry1 = new NavigationEntry("page_1", "Page 1", null, false);
            NavigationEntry entry2 = new NavigationEntry("page_2", "Page 2", null, false);
            MenuItem item = new MenuItem("Pages", List.of(entry1, entry2));
            menu.addItems(item);
            return menu;
        }

    }

    @BeforeEach
    public void setUp() {
        log.debug("Setting up StreamlitTests");
    }

    @Test
    public void testMultiPageApp() {
        StreamlitAppManager appManager = context -> new TestMultiPageApp();
        TestSupport.startServer(appManager);
    }


}
