package edu.boisestate.cs410.jeara.web.models;


import org.apache.commons.dbcp2.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by justinStiffler on 11/20/16.
 */
public class Tag {
    private static final Logger logger = LoggerFactory.getLogger(Tag.class);

    int tagId;
    String title;

    public Tag(int tagId, String title) {
        this.tagId = tagId;
        this.title = title;
    }

    public int getTagId() {
        return tagId;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "tagId=" + tagId +
                ", title='" + title + '\'' +
                '}';
    }

    public static long create (Request request, PoolingDataSource pool, Service http) throws SQLException{
        long tagId;

        String ttitle = request.queryParams("title");
        if(ttitle == null || ttitle.isEmpty()) {
            http.halt(400, "No Title Provided!");
        }

        String query =  "INSERT INTO tag ( title ) " +
                "VALUES (?) RETURNING tag_id";

        try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(query)) {
            stmt.setString(1, ttitle);

            stmt.execute();
            try(ResultSet set = stmt.getResultSet()) {
                set.next();
                tagId = set.getLong(1);
                logger.info("Successfully added new user");
            }
        }
        return tagId;
    }

    public static List<Tag> retrieveTags (PoolingDataSource pool) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        try(Connection cxn = pool.getConnection();
            Statement stmt = cxn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM tag;")){
            while(rs.next()){
                tags.add(new Tag(
                        rs.getInt("tag_id"),
                        rs.getString("title")));
            }
        }
        logger.info("Retrieved {} Tags", tags.size());
        return tags;
    }

    public static Tag getTag (PoolingDataSource pool, long tag_id) throws SQLException {
        String tagSelectString = "SELECT * FROM tag WHERE (tag_id = ?)";

        Tag tag = null;
        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(tagSelectString)) {
            ps.setLong(1, tag_id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tag = new Tag(
                            rs.getInt("tag_id"),
                            rs.getString("title")
                    );
                }
            }
        }
        return tag;
    }
}
