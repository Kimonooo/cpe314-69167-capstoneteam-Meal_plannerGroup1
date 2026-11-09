package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UndoStackTest {

    @Test
    void theLastActionIsUndoneFirst() {
        UndoStack<String> undo = new UndoStack<>();
        Stream.of("add Oats", "add Adobo", "remove Oats").forEach(undo::push);
        assertEquals(3, undo.size());
        assertEquals("remove Oats", undo.peek());
        assertEquals("remove Oats", undo.pop());
        assertEquals("add Adobo", undo.pop());
        assertEquals("add Oats", undo.pop());
        assertTrue(undo.isEmpty());
    }

    @Test
    void popOnAnEmptyStackGivesNull() {
        assertNull(new UndoStack<String>().pop());
    }
}
