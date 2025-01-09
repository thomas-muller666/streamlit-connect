package io.streamlitconnect;

import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppCache implements StreamlitAppManager {

    private final static Logger log = LoggerFactory.getLogger(AppCache.class);

    private final StreamlitAppManager appManager;

    // Use a ConcurrentHashMap to store the StreamlitApp instances by session ID

    private final ConcurrentHashMap<String, StreamlitApp> cache = new ConcurrentHashMap<>();

    public AppCache(@NonNull StreamlitAppManager appManager) {
        this.appManager = appManager;
    }

    @Override
    public @NotNull StreamlitApp getOrCreateApp(@NonNull StreamlitSessionContext context) {
        return cache.computeIfAbsent(context.getSessionId(), k -> appManager.getOrCreateApp(context));
    }

    @Override
    public void disposeSession(@NonNull String sessionId) {
        log.debug("Disposing app for session: {}", sessionId);
        StreamlitApp app = cache.remove(sessionId);
        if (app != null) {
            log.debug("App removed from cache, now closing it: {}", app);
            app.close();
            log.info("App instance closed/disposed {}", app);
        }
    }

    public void remove(@NonNull String sessionId) {
        cache.remove(sessionId);
    }

    public void clear() {
        log.info("Disposing all apps and clearing cache");
        for (StreamlitApp app : cache.values()) {
            try {
                app.close();
                log.info("App instance closed: {}", app);
            } catch (Exception e) {
                log.warn("Error '{}' closing app: {}", app, e.getMessage(), e);
            }
        }
        cache.clear();
    }
}
