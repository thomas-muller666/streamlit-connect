package io.streamlitconnect;

/**
 * Represent a container that is an inner container in a container.
 * See https://docs.streamlit.io/develop/api-reference/layout/st.container
 */
public interface InnerContainer extends Container {

    int getHeight();

    boolean isBorder();

}
