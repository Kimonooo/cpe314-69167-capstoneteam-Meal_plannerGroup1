package com.platenplatter.service;

import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Every rule for accounts: sign up, log in, change the display name, reset a forgotten
 * password with the security question, and delete the account. The screens only show what this class decides.
 */
public class AuthService {
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_NAME_LENGTH = 40;
    private static final String USERNAME_RULE = "[A-Za-z0-9._]{3,20}";

    /** The four questions a user can pick from when signing up. */
    public static final List<String> SECURITY_QUESTIONS = List.of(
            "What was the name of your first pet?",
            "In what city were you born?",
            "What is your favorite food?",
            "What was the name of your elementary school?");

    private final UserDao users;

    public AuthService(UserDao users) {
        this.users = users;
    }

    public User register(String displayName, String username, String ageText, String gender, String password,
                         String confirmPassword, String securityQuestion, String securityAnswer)
            throws AuthException, SQLException {
        String name = trim(displayName);
        String cleanUsername = trim(username);
        String answer = normalize(securityAnswer);
        if (name.isEmpty() || cleanUsername.isEmpty() || trim(ageText).isEmpty() || trim(gender).isEmpty()
                || password == null || password.isEmpty() || trim(securityQuestion).isEmpty() || answer.isEmpty()) {
            throw new AuthException("Please fill in every field.");
        }
        checkName(name);
        if (!cleanUsername.matches(USERNAME_RULE)) {
            throw new AuthException("Username must be 3 to 20 letters, numbers, dots or underscores (no spaces).");
        }
        int age = age(ageText);
        checkPassword(password, confirmPassword);
        if (!SECURITY_QUESTIONS.contains(securityQuestion)) {
            throw new AuthException("Please choose one of the security questions.");
        }
        if (users.findByUsername(cleanUsername).isPresent()) {
            throw new AuthException("That username is already taken. Please choose another.");
        }
        User user = new User(0, cleanUsername, name, age, trim(gender), securityQuestion);
        return users.insertUser(user, password, answer);
    }

    public User login(String username, String password) throws AuthException, SQLException {
        String cleanUsername = trim(username);
        if (cleanUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("Please enter your username and password.");
        }
        User user = users.findByUsername(cleanUsername).orElse(null);
        if (user == null || !users.passwordMatches(user.getUserId(), password)) {
            throw new AuthException("Incorrect username or password."); // never says which one was wrong
        }
        return user;
    }

    public User rename(User user, String newName) throws AuthException, SQLException {
        String clean = trim(newName);
        checkName(clean);
        if (!users.updateDisplayName(user.getUserId(), clean)) {
            throw new AuthException("Your account was not found. Please log in again.");
        }
        return user.withDisplayName(clean);
    }

    /**
     * Deletes the user's own account and everything saved for it (saved recipes, their ingredient
     * amounts and planned meals). The password must be typed again, so nobody else at the
     * keyboard can delete it. It cannot be undone.
     */
    public void deleteAccount(User user, String password) throws AuthException, SQLException {
        if (password == null || password.isEmpty()) {
            throw new AuthException("Type your password to delete your account.");
        }
        if (!users.passwordMatches(user.getUserId(), password)) {
            throw new AuthException("That password is not correct. Nothing was deleted.");
        }
        if (!users.deleteUser(user.getUserId())) {
            throw new AuthException("Your account was not found. Please log in again.");
        }
    }

    /** Step 1 of Forgot password: the question this username chose when signing up. */
    public String securityQuestionFor(String username) throws AuthException, SQLException {
        return users.findByUsername(trim(username)).map(User::getSecurityQuestion)
                .orElseThrow(() -> new AuthException("No account with that username."));
    }

    /** Step 2 of Forgot password: check the answer and the new password, then save a new hash. */
    public void resetPassword(String username, String answer, String newPassword, String confirmPassword)
            throws AuthException, SQLException {
        User user = users.findByUsername(trim(username)).orElseThrow(
                () -> new AuthException("No account with that username."));
        checkPassword(newPassword, confirmPassword); // 6+ characters, not only spaces, typed twice
        if (!users.answerMatches(user.getUserId(), normalize(answer))) {
            throw new AuthException("That answer does not match.");
        }
        users.updatePassword(user.getUserId(), newPassword);   // new PBKDF2 hash
    }

    /** " Bantay " and "bantay" are the same answer. */
    public static String normalize(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
    }

    private static void checkPassword(String password, String confirm) throws AuthException {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.isBlank()) {
            throw new AuthException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters, not only spaces.");
        }
        if (!password.equals(confirm)) {
            throw new AuthException("Passwords do not match.");
        }
    }

    private static void checkName(String name) throws AuthException {
        if (name.isEmpty()) {
            throw new AuthException("Please enter a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new AuthException("Name must be " + MAX_NAME_LENGTH + " characters or fewer.");
        }
    }

    private static int age(String ageText) throws AuthException {
        int age;
        try {
            age = Integer.parseInt(trim(ageText));
        } catch (NumberFormatException e) {
            throw new AuthException("Age must be a number.");
        }
        if (age < 1 || age > 120) {
            throw new AuthException("Age must be between 1 and 120.");
        }
        return age;
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
