package com.platenplatter.util;

import com.platenplatter.model.SavedRecipe;
import java.util.ArrayDeque;
import java.util.Queue;

/** A QUEUE of recipes to cook: first in, first out. Used by RecipeDemo. */
public class CookingQueue {
    private final Queue<SavedRecipe> queue = new ArrayDeque<>();

    public void add(SavedRecipe recipe) {
        queue.add(recipe);
    }

    /** The recipe that has waited longest, taken off the queue; null when it is empty. */
    public SavedRecipe next() {
        return queue.poll();
    }

    public SavedRecipe peek() {
        return queue.peek();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
