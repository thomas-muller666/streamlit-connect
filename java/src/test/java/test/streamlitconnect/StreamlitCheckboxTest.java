package test.streamlitconnect;

import io.streamlitconnect.OperationsRequestContext;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.Checkbox;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamlitCheckboxTest {

    private static final Logger log = LoggerFactory.getLogger(StreamlitCheckboxTest.class);

    private static class TestApp implements StreamlitApp {

        private final Checkbox checkbox1 = new Checkbox("Check me!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Checkbox 1 changed value callback: {}", this);
            }
        };

        private final Checkbox checkbox2 = new Checkbox("Check me!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Checkbox 2 changed value callback: {}", this);
            }
        };

        @Override
        public void render(@NonNull OperationsRequestContext context) {
            context.getRootContainer().title("Checkbox test");

            checkbox1.setLabel((checkbox1.getValue()) ? "Uncheck me!" : "Check me!");
            context.getRootContainer().widget(checkbox1);
            context.getRootContainer().text(checkbox1.getValue() ? "Checkbox 1 is checked!" : "Checkbox 1 is unchecked!");

            checkbox2.setLabel((checkbox2.getValue()) ? "Uncheck me!" : "Check me!");
            context.getRootContainer().widget(checkbox2);
            context.getRootContainer().text(checkbox2.getValue() ? "Checkbox 2 is checked!" : "Checkbox 2 is unchecked!");
        }
    }


    @Test
    public void testMultiPageApp() {
        StreamlitAppManager appManager = context -> new TestApp();
        TestSupport.startServer(appManager);
    }


}
