package edu.boisestate.cs410.jeara.web.models;

import org.apache.commons.dbcp2.PoolingDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by benneely on 11/7/16.
 */
public class BugSubscription {
    int bugId;
    int subscriberId;

    public BugSubscription(int bugId, int subscriberId) {
        this.bugId = bugId;
        this.subscriberId = subscriberId;
    }

    public int getBugId() {
        return bugId;
    }

    public int getSubscriptionId() {
        return subscriberId;
    }

    @Override
    public String toString() {
        return "TagSubscription{" +
                "bugId=" + bugId +
                ", subscriberId=" + subscriberId +
                '}';
    }

    public static BugSubscription getBugSubscription(PoolingDataSource pool, long bugId, long userId) throws SQLException {
        BugSubscription bugSubscription = null;

        String assigneeSelect = "SELECT * \n" +
                "FROM bugsubscription \n" +
                "WHERE bug_id = ? AND subscriber_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugId);
                ps.setLong(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bugSubscription = new BugSubscription(
                                rs.getInt("bug_id"),
                                rs.getInt("subscriber_id")
                        );
                    }
                }
            }
        }
        return bugSubscription;

    }

    public static boolean isBugSubscribed(PoolingDataSource pool, long bugId, long userId) throws SQLException {
        BugSubscription bugSubscription = null;

        String assigneeSelect = "SELECT * \n" +
                "FROM bugsubscription \n" +
                "WHERE bug_id = ? AND subscriber_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugId);
                ps.setLong(2, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                       return true;
                    }
                }
            }
        }
        return false;
    }

    public static void createBugSubscription(PoolingDataSource pool, long bugId, long userId) throws SQLException {
        BugSubscription bugSubscription = null;

        String assigneeSelect = "INSERT INTO bugsubscription (bug_id, subscriber_id)\n" +
                "VALUES (?,?);";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugId);
                ps.setLong(2, userId);
                ps.execute();
            }
        }
    }

    public static void destroyBugSubscription(PoolingDataSource pool, long bugId, long userId) throws SQLException {
        BugSubscription bugSubscription = null;

        String assigneeSelect = "DELETE FROM bugsubscription WHERE bug_id = ? AND subscriber_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugId);
                ps.setLong(2, userId);
                ps.execute();
            }
        }
    }
}
