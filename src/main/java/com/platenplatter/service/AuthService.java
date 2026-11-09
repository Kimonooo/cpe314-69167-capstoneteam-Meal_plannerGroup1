package com.platenplatter.service;

import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.util.PasswordHasher;

import java.sql.SQLException;

/**
 * Accounts: sign up, log in, and change the display name.
 * Responsibility: the RULES only (what is a valid username, password, age, name). The screens
 * show the messages; UserDao does the SQL; PasswordHasher does the salting and hashing.
 * Because it has no window, every rule can be tested with plain JUnit tests.
 *
 * The password is never saved: only a random salt and the PBKDF2 hash of (password + salt).
 */
public class AuthService {
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_NAME_LENGTH = 40;
    /** 3 to 20 letters, digits, dots or underscores. No spaces. */
    private static final String USERNAME_RULE = "[A-Za-z0-9._]{3,20}";

    private final UserDao users;

    public AuthService(UserDao users) {
        this.users = users;
    }

    /**
     * Checks the sign-up form and saves the new account.
     * @return the saved user (with the id the database gave it)
     * @throws AuthException with a message for the user when something in the form is wrong
     */
    public User register(String name, String username, String ageText, String gender,
                         String password, String confirmPassword) throws AuthException, SQLException {
        String cleanName = trim(name);
        String cleanUsername = trim(username);
        String cleanAge = trim(ageText);
        if (cleanName.isEmpty() || cleanUsername.isEmpty() || cleanAge.isEmpty()
                || gender == null || gender.isBlank() || password == null || password.isEmpty()) {
            throw new AuthException("Please fill in every field.");
        }
        checkName(cleanName);
        if (!cleanUsername.matches(USERNAME_RULE)) {
            throw new AuthException("Username must be 3 to 20 letters, numbers, dots or underscores (no spaces).");
        }
        int age;
        try {
            age = Integer.parseInt(cleanAge);
        } catch (NumberFormatException e) {
            throw new AuthException("Age must be a number.");
        }
        if (age < 1 || age > 120) {
            throw new AuthException("Age must be between 1 and 120.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new AuthException("Passwords do not match.");
        }
        if (users.findByUsername(cleanUsername) != null) {
            throw new AuthException("That username is already taken. Please choose another.");
        }
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt); // the real password is never saved
        users.insertUser(new User(0, cleanName, cleanUsername, age, gender, hash, salt));
        return users.findByUsername(cleanUsername);
    }

    /**
     * Logs a user in.
     * The message never says WHICH part was wrong, so nobody can find out which usernames exist.
     */
    public User login(String username, String password) throws AuthException, SQLException {
        String cleanUsername = trim(username);
        if (cleanUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("Please enter your username and password.");
        }
        User user = users.findByUsername(cleanUsername);
        if (user == null || !PasswordHasher.verify(password, user.getSalt(), user.getPasswordHash())) {
            throw new AuthException("Incorrect username or password.");
        }
        return user;
    }

    /**
     * Changes the display name shown in the header and saves it.
     * @return the same user with the new name
     */
    public User rename(User user, String newName) throws AuthException, SQLException {
        String clean = trim(newName);
        checkName(clean);
        if (!users.updateName(user.getId(), clean)) {
            throw new AuthException("Your account was not found. Please log in again.");
        }
        return user.withName(clean);
    }

    private static void checkName(String name) throws AuthException {
        if (name.isEmpty()) {
            throw new AuthException("Please enter a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new AuthException("Name must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
