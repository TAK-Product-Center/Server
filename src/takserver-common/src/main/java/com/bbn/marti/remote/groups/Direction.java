

package com.bbn.marti.remote.groups;

public enum Direction {
    IN(1),
    OUT(2);

    private int value;

    Direction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
