package test.streamlitconnect;

import static org.apache.commons.lang3.Validate.isTrue;

import io.streamlitconnect.Container;
import io.streamlitconnect.OperationsRequestContext;
import io.streamlitconnect.StreamlitApp;
import io.streamlitconnect.StreamlitAppManager;
import io.streamlitconnect.widgets.Button;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteStreamTest {

    private static class WordsWithSpacesIterator implements Iterator<String> {

        private final Scanner scanner;

        public WordsWithSpacesIterator(@NonNull String str) {
            this.scanner = new Scanner(str);
        }

        @Override
        public boolean hasNext() {
            return scanner.hasNext();

        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return scanner.next() + " "; // append the space at the end
        }
    }

    private static class DelayedIterator implements Iterator<String> {

        private final Random rand = new Random();

        private final Iterator<String> iterator;

        private final int minDelay;

        private final int maxDelay;

        public DelayedIterator(Iterator<String> iterator) {
            this(iterator, 1, 1000);
        }

        public DelayedIterator(@NonNull Iterator<String> iterator, int minDelay, int maxDelay) {
            isTrue(minDelay >= 0, "minDelay must be >= 0");
            isTrue(maxDelay >= minDelay, "maxDelay must be >= minDelay");
            this.iterator = iterator;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String chunk = iterator.next();

            // Sleep for a random time between 1 and 1000 milliseconds
            try {
                Thread.sleep(minDelay + rand.nextInt(maxDelay - minDelay));
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
            }

            return chunk;
        }
    }

    private static class StreamWriterApp implements StreamlitApp {

        private final Button button1 = new Button("Stream", "Stream some data") {

            @Override
            public void onChange(List<String> args, Map<String, String> kwargs) {
                log.debug("Button 1 click callback: {}", this);
            }
        };

        @Override
        public void render(@NotNull OperationsRequestContext context) {
            Container root = context.getRootContainer();
            root.title("WriteStream Test").widget(button1);

            if (button1.isChanged()) {
                DelayedIterator delayedIter = new DelayedIterator(new WordsWithSpacesIterator(LOREM_IPSUM));
                root.writeStream(delayedIter);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(WriteStreamTest.class);

    private static final String LOREM_IPSUM = """
            Lorem ipsum dolor sit amet, **consectetur adipiscing** elit, sed do eiusmod tempor
            incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis
            nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
        """;

    @Test
    public void testStreamWriterApp() {
        StreamlitAppManager appManager = context -> new StreamWriterApp();
        TestSupport.startServer(appManager);
    }

}
