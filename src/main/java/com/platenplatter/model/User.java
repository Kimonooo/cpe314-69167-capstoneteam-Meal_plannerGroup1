package com.platenplatter.model;

/**
 * A registered user. The password is never stored as plain text: only a salted hash.
 * "name" is the display name shown in the header; the user can change it on the Account tab.
 * The username (used to log in) never changes.
 */
public class User {
    private final int id;
    private final String name;
    private final String username;
    private final int age;
    private final String gender;
    private final String passwordHash;
    private final String salt;

    public User(int id, String name, String username, int age,
                String gender, String passwordHash, String salt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.age = age;
        this.gender = gender;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }

    /** A copy of this user with a new display name (User objects never change). */
    public User withName(String newName) {
        return new User(id, newName, username, age, gender, passwordHash, salt);
    }
}
