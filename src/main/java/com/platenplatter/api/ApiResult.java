package com.platenplatter.api;

import java.util.List;

/**
 * The answer of a TheMealDB call that never throws: OK with the meals, EMPTY when nothing
 * matched, or FAILED with the error (no internet, too many requests, or TheMealDB not
 * responding). The Recipes page shows a different message for each.
 */
public record ApiResult(Status status, List<MealData> meals, Throwable error) {

    public enum Status { OK, EMPTY, FAILED }

    public ApiResult {
        meals = meals == null ? List.of() : List.copyOf(meals);
    }

    /** OK when there are meals, EMPTY when the list is empty. */
    public static ApiResult of(List<MealData> meals) {
        return new ApiResult(meals == null || meals.isEmpty() ? Status.EMPTY : Status.OK, meals,
                null);
    }

    public static ApiResult failed(Throwable error) {
        return new ApiResult(Status.FAILED, List.of(), error);
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }
}
