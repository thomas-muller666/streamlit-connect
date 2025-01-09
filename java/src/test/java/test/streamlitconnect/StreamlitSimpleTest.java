package test.streamlitconnect;

import io.streamlitconnect.Container;
import io.streamlitconnect.OperationsRequestContext;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.widgets.Button;
import io.streamlitconnect.widgets.Checkbox;
import io.streamlitconnect.widgets.FloatRangeSlider;
import io.streamlitconnect.widgets.FloatSlider;
import io.streamlitconnect.widgets.IntegerRangeSlider;
import io.streamlitconnect.widgets.IntegerSlider;
import io.streamlitconnect.widgets.NumberInput;
import io.streamlitconnect.widgets.Radio;
import io.streamlitconnect.widgets.TextInput;
import io.streamlitconnect.widgets.Toggle;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamlitSimpleTest {

    private static final Logger log = LoggerFactory.getLogger(StreamlitSimpleTest.class);

    private static class SimpleApp implements StreamlitApp {

        private final Button button1 = new Button("Click me 1!", "Click this button to see what happens.") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Button 1 click callback: {}", this);
            }
        };

        private final Button button2 = new Button("Click me 2!", "Click this button to see what happens.") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Button 2 click callback: {}", this);
            }
        };

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

        private final Toggle toggle = new Toggle("Turn me on!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Toggle changed value callback: {}", this);
            }
        };

        private final Radio radio = new Radio("Select and option from this radio!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("Radio changed value callback: {}", this);
            }
        };

        private final NumberInput numberInput = new NumberInput("Enter a number!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("NumberInput changed value callback: {}", this);
            }
        };

        private final TextInput textInput = new TextInput("Enter some text!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("TextInput changed value callback: {}", this);
            }
        };

        private final IntegerSlider iSlider = new IntegerSlider("Select an integer value!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("IntegerSlider changed value callback: {}", this);
            }
        };

        private final FloatSlider fSlider = new FloatSlider("Select a float value!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("FloatSlider changed value callback: {}", this);
            }
        };

        private final IntegerRangeSlider iRangeSlider = new IntegerRangeSlider("Select an integer range!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("IntegerRangeSlider changed value callback: {}", this);
            }
        };

        private final FloatRangeSlider fRangeSlider = new FloatRangeSlider("Select a float range!") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                super.onChange(args, kwargs);
                log.debug("FloatRangeSlider changed value callback: {}", this);
            }
        };

        {
            radio.setOptions(new String[]{"Option 1", "Option 2", "Option 3"});
            radio.setCaptions(new String[]{"Caption 1", "Caption 2", "Caption 3"});

            numberInput.setMin(0);
            numberInput.setMax(100);
            numberInput.setStep(5);
        }

        @Override
        public void render(@NotNull OperationsRequestContext context) {
            Container root = context.getRootContainer();
            root.title("Hello, world!")
                .header("This is a header")
                .subheader("This is a subheader")
                .text("This is some text")
                .text("Session ID: " + context.getSessionContext().getSessionId())
                .markdown("This is some markdown text with **bold** and *italic* text.")
                .widget(button1)
                .widget(button2);

            if (button1.isChanged()) {
                root.text("Button 1 was clicked!");
            }

            if (button2.isChanged()) {
                root.text("Button 2 was clicked!");
            }

            root.widget(numberInput);
            root.text("NumberInput value: " + numberInput.getValue());

            root.widget(textInput);
            root.text("You entered: " + textInput.getValue());

            root.widget(iSlider);
            root.text("IntegerSlider value: " + iSlider.getValue());

            root.widget(fSlider);
            root.text("FloatSlider value: " + fSlider.getValue());

            root.widget(iRangeSlider);
            root.text("IntegerRangeSlider value: " + iRangeSlider.getValue());

            root.widget(fRangeSlider);
            root.text("FloatRangeSlider value: " + fRangeSlider.getValue());

            Container inner = root.innerContainer(true);
            inner.text("This is an inner container");

            checkbox1.setLabel((checkbox1.getValue()) ? "Uncheck me!" : "Check me!");
            root.widget(checkbox1);
            root.text(checkbox1.getValue() ? "Checkbox 1 is checked!" : "Checkbox 1 is unchecked!");

            checkbox2.setLabel((checkbox2.getValue()) ? "Uncheck me!" : "Check me!");
            root.widget(checkbox2);
            root.text(checkbox2.getValue() ? "Checkbox 2 is checked!" : "Checkbox 2 is unchecked!");

            toggle.setLabel((toggle.getValue()) ? "Turn me OFF!" : "Turn me ON!");
            root.widget(toggle);

            Container rootExpandable = root.expander("Expand me!");
            rootExpandable.text(toggle.getValue() ? "Toggle is ON!" : "Toggle is OFF!");

            Map<String, Container> tabs = root.tabs("Tab 1", "Tab 2", "Tab 3");
            Container tab1 = tabs.get("Tab 1");
            tab1.text("This is tab 1");

            Container tab2 = tabs.get("Tab 2");
            tab2.text("This is tab 2");

            Container tab3 = tabs.get("Tab 3");
            tab3.text("This is tab 3");

            List<Container> columns = root.columns(3);

            Container column1 = columns.getFirst();
            column1.text("This is column 1");

            Container column2 = columns.get(1);
            column2.text("This is column 2");

            Container column3 = columns.get(2);
            column3.text("This is column 3");

            Container sidebar = context.getSidebarContainer();
            sidebar.title("Sidebar title").header("Sidebar header");

            Container sidebarExpandable = sidebar.expander("Sidebar expander");
            sidebarExpandable.text("This is some text in the sidebar expander");

            Map<String, Container> sidebarTabs = sidebar.tabs("Tab A", "Tab B", "Tab C");
            Container tabA = sidebarTabs.get("Tab A");
            tabA.text("This is tab A");

            Container tabB = sidebarTabs.get("Tab B");
            tabB.text("This is tab B");

            Container tabC = sidebarTabs.get("Tab C");
            tabC.text("This is tab C");
        }
    }

    @BeforeEach
    public void setUp() {
        log.debug("Setting up StreamlitTests");
    }

    @Test
    public void testSimpleApp() {
        StreamlitAppManager appManager = context -> new SimpleApp();
        TestSupport.startServer(appManager);
    }


}
