package io.streamlitconnect;

/**
 * Represent a container that is expandable. See https://docs.streamlit.io/develop/api-reference/layout/st.expander
 */
public interface ExpandableContainer extends Container {

    /**
     * Get the label of the container.
     *
     * @return the label of the container
     */
    String getLabel();

    /**
     * Get whether the container is initially expanded.
     *
     * @return whether the container is initially expanded
     */
    boolean isInitiallyExpanded();

    /**
     * Get the icon of the container.
     *
     * @return the icon of the container
     */
    String getIcon();

}
