package edu.boisestate.cs410.jeara.web.models;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by benneely on 11/7/16.
 */
public class Milestone {
    private static final Logger logger = LoggerFactory.getLogger(Milestone.class);

    int milestoneId;
    String name;
    java.util.Date dueDate;
    Timestamp created;
    List<Bug> bugs = new ArrayList<Bug>();

    public int getMilestoneId() {
        return milestoneId;
    }

    public String getName() {
        return name;
    }

    public java.util.Date getDueDate() {
        return dueDate;
    }

    public String getPrettyDueDate() {
        DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        return formatter.format(dueDate);
    }

    public double getProgress() {
        int closedBugs = 0;

        for (Bug bug : this.bugs) {
            if (bug.isClosed()) {
                closedBugs += 1;
            }
        }

        return (Math.round((double)closedBugs/bugs.size() * 100d) / 100d) * 100;
    }

    public Timestamp getCreated() {
        return created;
    }

    public List<Bug> getBugs() {
        return bugs;
    }

    public void addBug (Bug bug) {
        this.bugs.add(bug);
    }

    public Milestone(int milestoneId, String name, java.util.Date dueDate, Timestamp created) {
        this.milestoneId = milestoneId;
        this.name = name;
        this.dueDate = dueDate;
        this.created = created;
    }

    @Override
    public String toString() {
        return "Milestone{" +
                "milestoneId=" + milestoneId +
                ", name='" + name + '\'' +
                ", dueDate=" + dueDate +
                ", created=" + created +
                '}';
    }

    public static List<Milestone> retrieveMilestones (PoolingDataSource pool) throws SQLException, ParseException {
        String milestoneSelectString =
                "SELECT *\n" +
                        "FROM milestone";

        List<Milestone> milestones = new ArrayList<>();

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(milestoneSelectString)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp createdTimestamp = null;
                    java.util.Date dueDate = null;

                    if (null != rs.getString("created")) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSSS");
                        java.util.Date date = dateFormat.parse(rs.getString("created"));
                        createdTimestamp = new java.sql.Timestamp(date.getTime());
                    }

                    if (null != rs.getString("due_date")) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        dueDate = dateFormat.parse(rs.getString("due_date"));
                    }


                    Milestone milestone = new Milestone (
                            rs.getInt("milestone_id"),
                            rs.getString("name"),
                            dueDate,
                            createdTimestamp
                    );

                    Milestone.addBugsToMilestone(cxn, milestone);
                    milestones.add(milestone);
                }
            }
        }
        return milestones;
    }

    public static Milestone retrieveMilestone (PoolingDataSource pool, int milestoneId) throws SQLException, ParseException {
        String milestoneSelectString = "SELECT * FROM milestone where milestone_id = ?";

        Milestone milestone = null;

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(milestoneSelectString)) {
            ps.setInt(1, milestoneId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp createdTimestamp = null;
                    java.util.Date dueDate = null;

                    if (null != rs.getString("created")) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSSS");
                        java.util.Date date = dateFormat.parse(rs.getString("created"));
                        createdTimestamp = new java.sql.Timestamp(date.getTime());
                    }

                    if (null != rs.getString("due_date")) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        dueDate = dateFormat.parse(rs.getString("due_date"));
                    }

                    milestone = new Milestone (
                            rs.getInt("milestone_id"),
                            rs.getString("name"),
                            dueDate,
                            createdTimestamp
                    );

                    Milestone.addBugsToMilestone(cxn, milestone);

                }
            }
        }
        return milestone;
    }


    public static int createMilestone (PoolingDataSource pool, String name, Date dueDate) throws SQLException {
        int milestoneId = -1;
        try (Connection cxn = pool.getConnection();) {
            boolean succeeded = false;
            cxn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            cxn.setAutoCommit(false);
            try {
                int retryCount = 5;
                while (retryCount > 0) {
                    try {
                        try (PreparedStatement gs = cxn.prepareStatement(
                                "INSERT INTO milestone (name, due_date)\n" +
                                        "VALUES (?,?)\n" +
                                        "RETURNING milestone_id")) {
                            gs.setString(1, name);
                            gs.setDate(2, dueDate);

                            gs.execute();
                            try (ResultSet rs = gs.getResultSet()) {
                                rs.next();
                                milestoneId = rs.getInt(1);
                            }
                            cxn.commit();
                            succeeded = true;
                            retryCount = 0;
                            logger.info("successfully added milestone");
                        }
                    } catch (SQLException ex) {
                        if (ex.getErrorCode() / 1000 == 23) {
                            logger.info("integrity error adding to database, retrying", ex);
                            retryCount--;
                        } else {
                            logger.info("other error encountered adding to database, aborting", ex);
                            throw ex;
                        }
                    } finally {
                        if (!succeeded) {
                            cxn.rollback();
                        }
                    }
                }
            } finally {
                cxn.setAutoCommit(true);
            }
        }
        return milestoneId;
    }

    public static void removeBugFromMilestone(PoolingDataSource pool, long bugId, long milestoneId) throws SQLException {
        String assigneeSelect = "UPDATE bug\n" +
                "SET milestone_id = null\n" +
                "WHERE bug_id = ? AND milestone_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugId);
                ps.setLong(2, milestoneId);
                ps.execute();
            }
        }
    }

    public static void addBugsToMilestone(Connection cxn, Milestone milestone) throws SQLException, ParseException {

        String bugSelectString = "SELECT * FROM bug where milestone_id = ?";
        PreparedStatement bugPs = cxn.prepareStatement(bugSelectString);
        bugPs.setInt(1, milestone.milestoneId);

        try (ResultSet bugRs = bugPs.executeQuery()) {
            while (bugRs.next()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSSS");
                Timestamp bugClosedTimestamp = null;
                Timestamp bugCreatedTimestamp = null;

                if (null != bugRs.getString("closed")) {
                    java.util.Date date = dateFormat.parse(bugRs.getString("closed"));
                    bugClosedTimestamp = new java.sql.Timestamp(date.getTime());
                }

                if (null != bugRs.getString("created")) {
                    java.util.Date date = dateFormat.parse(bugRs.getString("created"));
                    bugCreatedTimestamp = new java.sql.Timestamp(date.getTime());

                }

                Bug bug = new Bug(
                        bugRs.getInt("bug_id"),
                        bugRs.getInt("creator_id"),
                        bugRs.getInt("assignee_id"),
                        bugRs.getInt("milestone_id"),
                        bugRs.getString("title"),
                        bugRs.getString("details"),
                        bugRs.getString("status"),
                        bugClosedTimestamp,
                        bugCreatedTimestamp
                );

                Bug.addTags(cxn, bug);
                milestone.addBug(bug);
            }
        }
    }
}
