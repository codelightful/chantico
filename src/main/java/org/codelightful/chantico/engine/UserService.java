package org.codelightful.chantico.engine;

import org.codelightful.chantico.Chantico;
import org.codelightful.chantico.persistence.Operation;
import org.codelightful.harpo.RSAUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UserService {
    private static UserService instance = new UserService();

    public static UserService getInstance() {
        return instance;
    }

    /**
     * Inserts a new user record in the database
     * @param email User email
     * @param name User name (may be null)
     * @param password User password
     */
    public void createUser(String email, String name, String password) {
        Operation.Update.from("INSERT INTO chantico_users (user_email, user_name, user_password) VALUES (?, ?, ?)",
                email, name, password).execute();
    }

    /** Validates the user/password combination and returns the user name if it has been successful */
    public String authenticate(String login, String password) {
        if (login == null || password == null) {
            return null;
        }
        AtomicReference<String> userName = new AtomicReference<>();
        Operation.Query.from("SELECT user_password FROM chantico_users WHERE user_email = ?", login)
                .execute(rs -> {
                    String dbPassword = rs.getString("user_password");
                    if(Chantico.decrypt(dbPassword).equals(password)) {
                        userName.set(login);
                    }
                });
        return userName.get();
    }

    /** Allows to determine if any user is defined in the application */
    public boolean hasUsers() {
        AtomicBoolean result = new AtomicBoolean(false);
        Operation.Query.from("SELECT COUNT(*) FROM chantico_users").execute(rs -> {
            if (rs.getInt(1) > 0) {
                result.set(true);
            }
        });
        return result.get();
    }
}
