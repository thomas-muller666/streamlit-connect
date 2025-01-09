package test.streamlitconnect;

import io.streamlitconnect.Container;
import io.streamlitconnect.MultiPageApp;
import io.streamlitconnect.NavigationMenu;
import io.streamlitconnect.NavigationMenu.MenuItem;
import io.streamlitconnect.NavigationMenu.NavigationEntry;
import io.streamlitconnect.NavigationRequestContext;
import io.streamlitconnect.OperationsRequestContext;
import io.streamlitconnect.Page;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.LinkButton;
import io.streamlitconnect.widgets.PageLink;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamlitButtonTest {

    private static final Logger log = LoggerFactory.getLogger(StreamlitButtonTest.class);

    private static class TestApp implements StreamlitApp {

        private final Button button1 = new Button("Click me 1!", "Click button 1 to see what happens.") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Button 1 click callback: {}", this);
            }
        };

        private final Button button2 = new Button("Click me 2!", "Click button 2 to see what happens.") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Button 2 click callback: {}", this);
            }
        };

        @Override
        public void render(@NonNull OperationsRequestContext context) {
            context.getRootContainer().title("Button test");

            context.getRootContainer().widget(button1);
            context.getRootContainer().widget(button2);

            if (button1.isChanged()) {
                context.getRootContainer().text("Button 1 was clicked!");
            }

            if (button2.isChanged()) {
                context.getRootContainer().text("Button 2 was clicked!");
            }
        }
    }


    @Test
    public void testMultiPageApp() {
        StreamlitAppManager appManager = context -> new TestApp();
        TestSupport.startServer(appManager);
    }


}
