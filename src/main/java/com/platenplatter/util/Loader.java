package com.platenplatter.util;

/**
 * Tiny "give me some data" interface. Screens receive a Loader instead of talking to the
 * database directly, so a screen can be built and tested BEFORE the database class exists.
 * Example: dao::getPlannedCategories fits Loader<List<String>>.
 */
@FunctionalInterface
public interface Loader<T> {
    T load() throws Exception;
}
