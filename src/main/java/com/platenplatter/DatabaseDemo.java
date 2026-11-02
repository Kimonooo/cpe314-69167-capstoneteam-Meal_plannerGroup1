package com.platenplatter;

import com.platenplatter.db.Database;

/** Creates platenplatter.db and prints the tables, to prove the schema works. */
public class DatabaseDemo {
    public static void main(String[] args) throws Exception {
        Database.initialize();
        System.out.println("Tables in the database:");
        for (String table : Database.listTables()) {
            System.out.println(" - " + table);
        }
    }
}
