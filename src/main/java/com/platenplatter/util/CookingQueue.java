package com.platenplatter.util;

import com.platenplatter.model.Recipe;
import java.util.ArrayDeque;
import java.util.Queue;

/** First recipe added is the first one to be cooked (FIFO). */
public class CookingQueue {
    private final Queue<Recipe> queue = new ArrayDeque<>();

    public void add(Recipe recipe) {
        queue.add(recipe);
    }

    /** Removes and returns the next recipe to cook, or null if empty. */
    public Recipe next() {
        return queue.poll();
    }

    public Recipe peek() {
        return queue.peek();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
