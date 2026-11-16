package com.platenplatter.util;

import com.platenplatter.model.SavedRecipe;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * A QUEUE of recipes to cook: first in, first out. The Meal Plan page puts a day's meals in it in
 * cooking order (breakfast first), shows the one at the front, and "Done, next" takes it off.
 */
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

    /** Everything still waiting, front first, without taking anything off the queue. */
    public List<SavedRecipe> inOrder() {
        return List.copyOf(queue);
    }
}
