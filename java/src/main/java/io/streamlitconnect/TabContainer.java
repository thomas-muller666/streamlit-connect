package io.streamlitconnect;

/**
 * Represent a container that is a tab in a tabbed container. Each tab is associated with a name. See {link Container#tabs}.
 */
public interface TabContainer extends Container {

    /**
     * Get the name of the tab.
     *
     * @return the name of the tab
     */
    String getName();

}
