package edu.boisestate.cs410.jeara.web.models;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by benneely on 11/7/16.
 */
public class Comment {
    private static final Logger logger = LoggerFactory.getLogger(Comment.class);
    int commentId;
    int bugId;
    int creatorId;
    String creatorEmail;
    String body;
    Timestamp created;

    String creatorName;

    public Comment(int commentId, int bugId, int creatorId, String body, Timestamp created) {
        this.commentId = commentId;
        this.bugId = bugId;
        this.creatorId = creatorId;
        this.creatorName = "unknown";
        this.body = body;
        this.created = created;

    }

    /*
     * additonal constructor to add commentor's name to the object.
     */
    public Comment(int commentId, int bugId, int creatorId, String creatorName, String creatorEmail, String body, Timestamp created) {
        this.commentId = commentId;
        this.bugId = bugId;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.creatorEmail = creatorEmail;
        this.body = body;
        this.created = created;

    }


    public int getCommentId() {
        return commentId;
    }

    public int getBugId() {
        return bugId;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public String getBody() {
        return body;
    }

    public String getcreatorEmail() {
        return creatorEmail;
    }

    public Timestamp getCreated() {
        return created;
    }

    public String getGravatarUrl () {
        String hash = Comment.md5Hex(this.getcreatorEmail());
        return "https://www.gravatar.com/avatar/" + hash;
    }

    public String getPrettyCreatedDate() {
        DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(created);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentId=" + commentId +
                ", bugId=" + bugId +
                ", creatorId=" + creatorId +
                ", body='" + body + '\'' +
                ", created=" + created +
                '}';
    }

    public static long create(Request request, PoolingDataSource pool, Service http) throws SQLException {
        long comId;

        long bugId = Integer.parseInt(request.queryParams("bug_id"));

        long commieCreator = Integer.parseInt(request.queryParams("creator_id"));

        String body = request.queryParams("body");
        if (body == null || body.isEmpty()) {
            http.halt(400, "No comment body provided!");
        }

        String query = "INSERT INTO comment (bug_id, creator_id, body) " +
                "VALUES (?, ?, ?) RETURNING comment_id";

        try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(query)) {
            stmt.setLong(1, bugId);
            stmt.setLong(2, commieCreator);
            stmt.setString(3, body);
            stmt.execute();
            try (ResultSet set = stmt.getResultSet()) {
                set.next();
                comId = set.getLong(1);
            }
        }
        return comId;
    }


    public static List<Comment> retrieveComments(PoolingDataSource pool, long bug_id) throws SQLException {
        String commentSelectString =
                "SELECT comment_id, bug_id, creator_id, name, email, body, comment.created " +
                        "FROM comment\n" +
                        "  JOIN j_user ON (creator_id = j_user.user_id)\n" +
                        "WHERE (bug_id = ?)" +
                        "ORDER BY created ASC";

        List comments = new ArrayList<>();

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(commentSelectString)) {
            ps.setLong(1, bug_id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Comment comment = new Comment(
                            rs.getInt("comment_id"),
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("body"),
                            rs.getTimestamp("created")
                    );
                    comments.add(comment);
                }
            }
        }
        return comments;
    }


    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }

    public static String md5Hex(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(message.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

}
