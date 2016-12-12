package edu.boisestate.cs410.jeara.web.models;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by benneely on 11/7/16.
 */
public class User {
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    int userId;
    String username;
    String email;
    String name;
    String pwHash;
    String created;

    public User(int userId, String email, String name, String username, String pwHash, String created) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.username = username;
        this.pwHash = pwHash;
        this.created = created;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPwHash() {
        return pwHash;
    }

    public String getCreated() {
        return created;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", created=" + created +
                '}';
    }

    public static long create(Request request, PoolingDataSource pool, Service http) throws SQLException {
        long userId;

        String name = request.queryParams("username");
        if (name == null || name.isEmpty()) {
            http.halt(400, "No Username Provided!");
        }

        String displayname = request.queryParams("name");
        if (displayname == null || displayname.isEmpty()) {
            http.halt(400, "No name Provided!");
        }

        String email = request.queryParams("email");
        if (email == null || email.isEmpty()) {
            http.halt(400, "No email Provided!");
        }

        String password = request.queryParams("password");
        if (password == null || password.isEmpty()) {
            http.halt(400, "No Password Provided!");
        }

        if (!password.equals(request.queryParams("confirm"))) {
            http.halt(400, "Password Did Not Match Confirmation!");
        }

        String pw = BCrypt.hashpw(password, BCrypt.gensalt(10));

        String query = "INSERT INTO j_user (username, name, email, pw_hash) " +
                "VALUES (?, ?, ?, ?) RETURNING user_id";


        try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, displayname);
            stmt.setString(3, email);
            stmt.setString(4, pw);
            stmt.execute();
            try (ResultSet set = stmt.getResultSet()) {
                set.next();
                userId = set.getLong(1);
                logger.info("Successfully added new user");
            }
        }
        return userId;
    }

    /**
     * Log a user into the application if their credentials are valid
     *
     * @param request The request object containing the parameters
     * @param pool    The connection pool to grab a connection from
     * @param http    The Service object
     * @return The user's id if their credentials were valid, 0 otherwise
     * @throws SQLException
     */
    public static long login(Request request, PoolingDataSource pool, Service http) throws SQLException {
        long userId = 0;

        String username = request.queryParams("username");
        if (username == null || username.isEmpty()) {
            http.halt(400, "No username provided!");
        }

        String password = request.queryParams("password");
        if (password == null || password.isEmpty()) {
            http.halt(400, "No password provided!");
        }

        String query = "SELECT user_id, pw_hash FROM j_user WHERE username = ?";

        try (Connection cxn = pool.getConnection();
             PreparedStatement stmt = cxn.prepareStatement(query)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String pwHash = rs.getString("pw_hash");
                    if (BCrypt.checkpw(password, pwHash)) {
                        userId = rs.getLong("user_id");
                        //enable a session so we can remember that the user has logged in
                        request.session(true).attribute("userId", userId);
                    }
                }
            }
        }
        return userId;
    }

    /**
     * Log a user out of the application
     *
     * @param request The request object containing the user's current sesion
     */
    public static void logout(Request request) {
        request.session().removeAttribute("userId");
        request.session().invalidate();
    }

    public static User retrieveUser(PoolingDataSource pool, long userId) throws SQLException {
        String userSelectString =
                "SELECT * FROM j_user\n" +
                        "WHERE user_id = ?";

        User user = null;

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(userSelectString)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = new User(
                            rs.getInt("user_id"),
                            rs.getString("email"),
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("pw_hash"),
                            rs.getString("created")
                    );
                }
            }
        }
        return user;
    }

    public static List<User> retrieveUsers(PoolingDataSource pool) throws SQLException {
        String userSelectString =
                "SELECT * FROM j_user";

        List<User> users = new ArrayList<>();

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(userSelectString)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                            rs.getInt("user_id"),
                            rs.getString("email"),
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("pw_hash"),
                            rs.getString("created")
                    ));
                }
            }
        }
        return users;
    }
}
