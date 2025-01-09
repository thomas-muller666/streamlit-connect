package io.streamlitconnect;


/**
 * StreamlitContext interface represents the context for a Streamlit req/res transaction, i.e. a single top-down application
 * script execution.
 */
public interface OperationsRequestContext extends StreamlitRequestContext {

    /**
     * This method returns the name of the current page in the Streamlit req/res transaction.
     *
     * @return The name of the current page in the Streamlit application session.
     */
    String getPageName();

    /**
     * This method returns a Container object that represents the current page in the Streamlit req/res transaction.
     *
     * @return The container to be used for the current page in the Streamlit application session.
     */
    Container getRootContainer();

    /**
     * This method returns a Container object that represents the sidebar in the Streamlit req/res transaction. A sidebar is only
     * generated in the application if the returned container is actually written to.
     *
     * @return The sidebar Container for the Streamlit application session.
     */
    Container getSidebarContainer();
}
