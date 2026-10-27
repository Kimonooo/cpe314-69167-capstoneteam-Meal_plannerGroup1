package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.MainCourseRecipe;
import com.platenplatter.model.Recipe;
import org.junit.jupiter.api.Test;

class CookingQueueTest {

    @Test
    void recipesComeOutInTheOrderTheyWereAdded() {
        CookingQueue queue = new CookingQueue();
        Recipe first = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        Recipe second = new MainCourseRecipe(2, "Pasta Pomodoro", "Italian", 20, 550);
        queue.add(first);
        queue.add(second);

        assertSame(first, queue.next());
        assertSame(second, queue.next());
        assertTrue(queue.isEmpty());
    }

    @Test
    void nextOnAnEmptyQueueReturnsNull() {
        CookingQueue queue = new CookingQueue();
        assertNull(queue.next()); // edge case: nothing to cook
    }
}
