package com.platenplatter.controller;

import com.platenplatter.model.User;
import com.platenplatter.service.AuthException;
import com.platenplatter.service.AuthService;
import java.sql.SQLException;
import java.util.List;

/**
 * The controller between the account screens (Log in, Sign up, Forgot password, Account) and
 * AuthService. Each screen handler calls ONE method here; the rules themselves stay in
 * AuthService. Call these methods from a background Task: hashing a password is slow on purpose.
 */
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public User logIn(String username, String password) throws AuthException, SQLException {
        return auth.login(username, password);
    }

    public User signUp(SignUpForm form) throws AuthException, SQLException {
        return auth.register(form.displayName(), form.username(), form.age(), form.gender(),
                form.password(), form.confirmPassword(), form.question(), form.answer());
    }

    public String questionFor(String username) throws AuthException, SQLException {
        return auth.securityQuestionFor(username);
    }

    public void resetPassword(String username, String answer, String newPassword, String confirm)
            throws AuthException, SQLException {
        auth.resetPassword(username, answer, newPassword, confirm);
    }

    public User rename(User user, String newName) throws AuthException, SQLException {
        return auth.rename(user, newName);
    }

    public void deleteAccount(User user, String password) throws AuthException, SQLException {
        auth.deleteAccount(user, password);
    }

    public List<String> securityQuestions() {
        return AuthService.SECURITY_QUESTIONS;
    }

    /**
     * Everything typed on the Sign up page, read on the UI thread and handed to the background
     * task.
     */
    public record SignUpForm(String displayName, String username, String age, String gender,
                             String password, String confirmPassword, String question,
                                     String answer) { }
}
