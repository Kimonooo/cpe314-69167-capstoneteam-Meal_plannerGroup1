package com.platenplatter;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.MainCourseRecipe;
import com.platenplatter.model.PrepTier;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.util.CircularRecipeException;
import com.platenplatter.util.CookingQueue;
import com.platenplatter.util.RecipeCalculator;
import com.platenplatter.util.UndoStack;
import java.util.stream.Stream;

/**
 * A console demo of the data structures and the recursion (Run As -> Java Application):
 * a QUEUE of recipes to cook, an undo STACK, and the recursive total of a nested recipe.
 */
public class RecipeDemo {
    public static void main(String[] args) {
        // Every variable is typed SavedRecipe, but each one behaves like its own subclass
        SavedRecipe oats = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        SavedRecipe pasta = new MainCourseRecipe(2, "Pasta Pomodoro", "Italian", 20, 550);
        SavedRecipe cake = new DessertRecipe(3, "Chocolate Cake", "American", 30, 400);

        CookingQueue queue = new CookingQueue();
        Stream.of(oats, pasta, cake).forEach(queue::add);
        System.out.println("Queue (first in, first out):");
        Stream.generate(queue::next).limit(queue.size()).forEach(r -> System.out.println("  " + r));

        UndoStack<String> undo = new UndoStack<>();
        Stream.of("add Overnight Oats", "add Pasta Pomodoro", "remove Overnight Oats").forEach(undo::push);
        System.out.println("Undo stack (last in, first out): undo \"" + undo.pop() + "\" first");

        // Recursion: Cake (Dessert, base 30) holds Filling (Main Course, base 20) holds Cream (Breakfast, base 10)
        SavedRecipe filling = new MainCourseRecipe(4, "Filling", "", 20, 450);
        SavedRecipe cream = new BreakfastRecipe(5, "Cream", "", 10, 400);
        filling.addSubRecipe(cream);
        cake.addSubRecipe(filling);
        int total = RecipeCalculator.totalPrepMinutes(cake);
        System.out.println("Cake with filling and cream: " + total + " min (" + PrepTier.of(total) + "), "
                + RecipeCalculator.totalCalories(cake) + " kcal");
        try {
            cream.addSubRecipe(cake); // the cream already sits inside the cake
        } catch (CircularRecipeException e) {
            System.out.println("Refused: " + e.getMessage());
        }
    }
}
