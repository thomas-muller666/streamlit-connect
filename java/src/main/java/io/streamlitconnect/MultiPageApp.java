package io.streamlitconnect;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for Streamlit applications with one or more pages.
 */
public interface MultiPageApp extends StreamlitApp {

    Logger log = LoggerFactory.getLogger(MultiPageApp.class);

    /**
     * Get the page with the given name.
     *
     * @param name The name of the page.
     * @return The page.
     */
    @NonNull
    Page getPage(@NonNull String name);

    /**
     * Renders the current page using the given Streamlit context. See {@link OperationsRequestContext#getPageName()}.
     *
     * @param context The Streamlit context.
     */
    @Override
    default void render(@NonNull OperationsRequestContext context) {
        log.debug("render: {}", context);
        String pageName = context.getPageName();
        log.debug("pageName: {}", pageName);
        if (pageName == null) {
            log.warn( "Cannot render a page without a name for context: {}", context);
            throw new StreamlitException("Cannot render a page without a name for context: " + context);
        }
        Page page = getPage(pageName);
        page.render(context);
    }

}
