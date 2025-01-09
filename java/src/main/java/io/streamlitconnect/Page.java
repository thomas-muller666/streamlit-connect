package io.streamlitconnect;

/**
 * Represents a page in a multi-page Streamlit application.
 */
public interface Page {

    /**
     * The unique (internal) name of the page.
     *
     * @return The unique (internal) name of the page.
     */
    String getName();

    /**
     * Render the page using the given Streamlit context.
     *
     * @param context The Streamlit context.
     */
    void render(OperationsRequestContext context);

}

