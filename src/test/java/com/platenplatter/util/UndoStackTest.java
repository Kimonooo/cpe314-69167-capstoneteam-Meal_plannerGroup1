package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UndoStackTest {

    @Test
    void lastActionPushedIsTheFirstUndone() {
        UndoStack<String> stack = new UndoStack<>();
        stack.push("add breakfast");
        stack.push("add lunch");
        stack.push("move dinner");

        assertEquals("move dinner", stack.pop());
        assertEquals("add lunch", stack.pop());
        assertEquals("add breakfast", stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void popOnAnEmptyStackReturnsNull() {
        // edge case: nothing to undo
        UndoStack<String> stack = new UndoStack<>();
        assertNull(stack.pop());
    }
}
