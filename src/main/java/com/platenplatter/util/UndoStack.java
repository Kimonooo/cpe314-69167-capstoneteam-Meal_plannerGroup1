package com.platenplatter.util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Last action in, first action out (LIFO), used for "undo". */
public class UndoStack<T> {
    private final Deque<T> stack = new ArrayDeque<>();

    public void push(T action) {
        stack.push(action);
    }

    /** Removes and returns the most recent action, or null if empty. */
    public T pop() {
        return stack.poll();
    }

    public T peek() {
        return stack.peek();
    }

    public int size() {
        return stack.size();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
