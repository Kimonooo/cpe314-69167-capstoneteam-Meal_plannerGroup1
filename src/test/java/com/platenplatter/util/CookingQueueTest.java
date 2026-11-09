package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.SavedRecipe;
import org.junit.jupiter.api.Test;

class CookingQueueTest {

    @Test
    void recipesComeOutInTheOrderTheyWentIn() {
        CookingQueue queue = new CookingQueue();
        SavedRecipe oats = new BreakfastRecipe(1, "Overnight Oats", "", 10, 300);
        SavedRecipe cake = new DessertRecipe(2, "Chocolate Cake", "", 30, 400);
        queue.add(oats);
        queue.add(cake);
        assertEquals(2, queue.size());
        assertEquals(oats, queue.peek());
        assertEquals(oats, queue.next());
        assertEquals(cake, queue.next());
        assertTrue(queue.isEmpty());
    }

    @Test
    void anEmptyQueueGivesNullInsteadOfCrashing() {
        assertNull(new CookingQueue().next());
        assertNull(new CookingQueue().peek());
    }
}
