package com.platenplatter;

import com.platenplatter.db.Database;
import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.util.PasswordHasher;

/** Live proof for the demo: one INSERT and one SELECT against the real database file. */
public class UserDaoDemo {
    public static void main(String[] args) throws Exception {
        Database.initialize();
        UserDao dao = new UserDao();

        String username = "demo" + System.currentTimeMillis(); // new name every run
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("secret123", salt);

        dao.insertUser(new User(0, "Demo User", username, 20, "F", hash, salt));
        System.out.println("INSERT worked for: " + username);

        User found = dao.findByUsername(username);
        System.out.println("SELECT found: " + found.getName() + " (id " + found.getId() + ")");
        System.out.println("Right password accepted: "
                + PasswordHasher.verify("secret123", found.getSalt(), found.getPasswordHash()));
        System.out.println("Wrong password accepted: "
                + PasswordHasher.verify("wrong", found.getSalt(), found.getPasswordHash()));
        System.out.println("Unknown user gives: " + dao.findByUsername("nobody-here"));
    }
}
