package com.platenplatter.model;

/**
 * One row of the users table.
 * Every column has a field with the same name in camelCase (display_name is displayName).
 * password_hash, salt and security_answer_hash are NOT here: they stay inside UserDao,
 * so no screen can ever see them.
 */
public class User {
    private final long userId;
    private final String username;
    private final String displayName;
    private final int age;
    private final String gender;
    private final String securityQuestion;

    public User(long userId, String username, String displayName, int age, String gender, String securityQuestion) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.age = age;
        this.gender = gender;
        this.securityQuestion = securityQuestion;
    }

    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getSecurityQuestion() { return securityQuestion; }

    /** A copy with a new display name (a User never changes after it is made). */
    public User withDisplayName(String newDisplayName) {
        return new User(userId, username, newDisplayName, age, gender, securityQuestion);
    }

    @Override
    public String toString() {
        return displayName + " (" + username + ")";
    }
}
