package edu.boisestate.cs410.jeara.web.models;

import org.apache.commons.dbcp2.PoolingDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by benneely on 11/7/16.
 */
public class TagSubscription {
    int tagId;
    int subscriberId;

    public TagSubscription(int tagId, int subscriberId) {
        this.tagId = tagId;
        this.subscriberId = subscriberId;
    }

    public int getBugId() {
        return tagId;
    }

    public int getSubscriptionId() {
        return subscriberId;
    }

    @Override
    public String toString() {
        return "TagSubscription{" +
                "tagId=" + tagId +
                ", subscriberId=" + subscriberId +
                '}';
    }

    public static TagSubscription getTagSubscription(PoolingDataSource pool, long tagId, long userId) throws SQLException {
        TagSubscription tagSubscription = null;

        String assigneeSelect = "SELECT * \n" +
                "FROM tagsubscription \n" +
                "WHERE tag_id = ? AND subscriber_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, tagId);
                ps.setLong(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tagSubscription = new TagSubscription(
                                rs.getInt("tag_id"),
                                rs.getInt("subscriber_id")
                        );
                    }
                }
            }
        }
        return tagSubscription;

    }

    public static void createTagSubscription(PoolingDataSource pool, long tagId, long userId) throws SQLException {
        BugSubscription bugSubscription = null;

        String assigneeSelect = "INSERT INTO tagsubscription (tag_id, subscriber_id)\n" +
                "VALUES (?,?);";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, tagId);
                ps.setLong(2, userId);
                ps.execute();
            }
        }
    }

    public static void destroyTagSubscription(PoolingDataSource pool, long tagId, long userId) throws SQLException {
        String assigneeSelect = "DELETE FROM tagsubscription WHERE tag_id = ? AND subscriber_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, tagId);
                ps.setLong(2, userId);
                ps.execute();
            }
        }
    }
}
