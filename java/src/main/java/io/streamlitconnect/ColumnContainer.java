package io.streamlitconnect;

/**
 * Represent a container that is a column in a column container.
 */
public interface ColumnContainer extends Container {

    /**
     * Enum representing the gap between columns.
     */
    enum ColumnGap {
        SMALL,
        MEDIUM,
        LARGE,
    }

    /**
     * Enum representing the vertical alignment of the column.
     */
    enum ColumnVerticalAlignment {
        TOP,
        CENTER,
        BOTTOM,
    }

    /**
     * Get the column index. The first column is 0.
     *
     * @return the column index
     */
    int getColumn();

}
