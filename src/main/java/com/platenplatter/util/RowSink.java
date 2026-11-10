package com.platenplatter.util;

/**
 * Where an imported row goes. The database person implements this with a
 * PreparedStatement INSERT; tests can implement it with a simple counter.
 */
@FunctionalInterface
public interface RowSink {
    void accept(ImportRow row) throws Exception;
}
