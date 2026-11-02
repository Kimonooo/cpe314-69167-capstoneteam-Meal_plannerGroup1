package com.platenplatter;

import com.platenplatter.model.*;
import com.platenplatter.util.CookingQueue;

public class RecipeDemo {
    public static void main(String[] args) {
        CookingQueue queue = new CookingQueue();

        // Every variable below is typed "Recipe", but each behaves differently
        Recipe oats = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        Recipe pasta = new MainCourseRecipe(2, "Pasta Pomodoro", "Italian", 20, 550);
        Recipe cake = new DessertRecipe(3, "Chocolate Cake", "American", 30, 400);

        queue.add(oats);
        queue.add(pasta);
        queue.add(cake);

        while (!queue.isEmpty()) {
            Recipe next = queue.next();
            System.out.println(next); // calls the subclass's getPrepMinutes()
        }
    }
}
