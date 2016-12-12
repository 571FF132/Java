package edu.boisestate.cs410.jeara.web.models;

import org.apache.commons.dbcp2.PoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Service;

import javax.activation.DataSource;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by benneely on 11/7/16.
 */
public class Bug {
    private static final Logger logger = LoggerFactory.getLogger(Bug.class);

    int bugId;
    int creatorId;
    int assigneeId;
    int milestoneId;
    String title;
    String details;
    String status;
    Timestamp closed;
    Timestamp created;
    List<Tag> tags = new ArrayList<Tag>();

    User assignee;

    public Bug(int bugId, int creatorId, int assigneeId, int milestoneId, String title, String details, String status, Timestamp closed, Timestamp created) {
        this.bugId = bugId;
        this.creatorId = creatorId;
        this.assigneeId = assigneeId;
        this.milestoneId = milestoneId;
        this.title = title;
        this.details = details;
        this.status = status;
        this.closed = closed;
        this.created = created;
    }

    public int getBugId() {
        return bugId;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public int getAssigneeId() {
        return assigneeId;
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getClosed() {
        return closed;
    }

    public boolean isClosed() {
        return status.equals("closed");
    }

    public Timestamp getCreated() {
        return created;
    }

    public User getAssignee() {
        return assignee;
    }

    public List<Tag> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "Bug{" +
                "bugId=" + bugId +
                ", creatorId=" + creatorId +
                ", assigneeId=" + assigneeId +
                ", milestoneId=" + milestoneId +
                ", title='" + title + '\'' +
                ", details='" + details + '\'' +
                ", status='" + status + '\'' +
                ", closed=" + closed +
                ", created=" + created +
                '}';
    }

    public static long create(Request request, PoolingDataSource pool, Service http) throws SQLException {
        long bugId;

        long bcreator = Integer.parseInt(request.queryParams("creator_id"));

        String btitle = request.queryParams("title");
        if (btitle == null || btitle.isEmpty()) {
            http.halt(400, "No Username Provided!");
        }

        String bdetails = request.queryParams("details");
        if (bdetails == null || bdetails.isEmpty()) {
            http.halt(400, "No name Provided!");
        }

        String bstatus = "open";

        String query = "INSERT INTO bug (creator_id, title, details, status) " +
                "VALUES (?, ?, ?, ?) RETURNING bug_id";

        try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(query)) {
            stmt.setLong(1, bcreator);
            stmt.setString(2, btitle);
            stmt.setString(3, bdetails);
            stmt.setString(4, bstatus);
            stmt.execute();
            try (ResultSet set = stmt.getResultSet()) {
                set.next();
                bugId = set.getLong(1);
                logger.info("Successfully added new bug");
            }
        }

        //add the new bug to our search table
        String searchQuery = "INSERT INTO bug_search (bug_id, bug_vector, current) " +
        "SELECT bug_id, " +
        "setweight(to_tsvector(title), 'A') " +
                "|| setweight(to_tsvector(coalesce(details, '')), 'B') " +
                "|| setweight(to_tsvector(coalesce(status, '')), 'C'), " +
                "TRUE " +
        "FROM bug " +
        "WHERE bug_id = ?";

        try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(searchQuery)) {
            stmt.setLong(1, bugId);
            stmt.execute();
        }

        return bugId;
    }

    /*
    * UPDATE AN EXISTING BUG. COPYING THE SMART GUYS.
    */
    public static long edit(Request request, PoolingDataSource pool, Service http) throws SQLException {
        String createQuery;

        long bug_id = Integer.parseInt(request.queryParams("bug_id"));

        if (Integer.parseInt(request.queryParams("assignee_id")) == 0) {
            createQuery = "UPDATE bug " +
                    "SET assignee_id = null " +
                    "WHERE (bug_id = ?)";
            try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(createQuery)) {
                stmt.setLong(1, bug_id);
                stmt.execute();
            }
        } else {
            createQuery = "UPDATE bug " +
                    "SET assignee_id = ? " +
                    "WHERE (bug_id = ?)";
            try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(createQuery)) {
                stmt.setLong(1, Integer.parseInt(request.queryParams("assignee_id")));
                stmt.setLong(2, bug_id);
                stmt.execute();
            }
        }

        if (Integer.parseInt(request.queryParams("milestone_id")) == 0) {
            createQuery = "UPDATE bug " +
                    "SET milestone_id = null " +
                    "WHERE (bug_id = ?)";
            try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(createQuery)) {
                stmt.setLong(1, bug_id);
                stmt.execute();
            }
        } else {
            createQuery = "UPDATE bug " +
                    "SET milestone_id = ? " +
                    "WHERE (bug_id = ?)";
            try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(createQuery)) {
                stmt.setLong(1, Integer.parseInt(request.queryParams("milestone_id")));
                stmt.setLong(2, bug_id);
                stmt.execute();
            }
        }

        String btitle = request.queryParams("title");
        if (btitle == null || btitle.isEmpty()) {
            http.halt(400, "No title Provided!");
        }

        String bdetails = request.queryParams("details");
        if (bdetails == null || bdetails.isEmpty()) {
            http.halt(400, "No details Provided!");
        }
        String bstatus = request.queryParams("status");
        if (bstatus == null || bstatus.isEmpty()) {
            http.halt(400, "No status Provided!");
        }

        if (bstatus.equals("closed")) {
            createQuery = "UPDATE bug " +
                    "SET title = ?, details = ?, status = ?, closed = current_timestamp " +
                    "WHERE (bug_id = ?)";
        } else {
            createQuery = "UPDATE bug " +
                    "SET title = ?, details = ?, status = ?, closed = null " +
                    "WHERE (bug_id = ?)";
        }

        try (Connection cxn = pool.getConnection(); PreparedStatement stmt = cxn.prepareStatement(createQuery)) {
            stmt.setString(1, btitle);
            stmt.setString(2, bdetails);
            stmt.setString(3, bstatus);
            stmt.setLong(4, bug_id);
            stmt.execute();
        }
        return bug_id;
    }


    public static List<Bug> retrieveBugsAssignedToUser(PoolingDataSource pool, long userId) throws SQLException, ParseException {
        String userSelectString =
                "SELECT *\n" +
                        "FROM bug\n" +
                        "WHERE assignee_id = ?";

        List<Bug> bugs = new ArrayList<>();

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(userSelectString)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bug bug = new Bug(
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getInt("assignee_id"),
                            rs.getInt("milestone_id"),
                            rs.getString("title"),
                            rs.getString("details"),
                            rs.getString("status"),
                            rs.getTimestamp("closed"),
                            rs.getTimestamp("created")
                    );
                    Bug.addTags(cxn, bug);
                    bugs.add(bug);
                }
            }
        }
        return bugs;
    }

    /*
     * Returns list of Bugs sorted by default creation date
     */
    public static List<Bug> bugList(PoolingDataSource pool) throws SQLException {
        List<Bug> bugs = new ArrayList<>();
        try (Connection cxn = pool.getConnection();
             Statement stmt = cxn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT bug_id, creator_id, assignee_id, " +
                     "milestone_id, title, details, status, closed, created " +
                     "FROM bug " +
                     "ORDER BY created ASC;")) {
            while (rs.next()) {
                Bug bug = new Bug(
                        rs.getInt("bug_id"),
                        rs.getInt("creator_id"),
                        rs.getInt("assignee_id"),
                        rs.getInt("milestone_id"),
                        rs.getString("title"),
                        rs.getString("details"),
                        rs.getString("status"),
                        rs.getTimestamp("closed"),
                        rs.getTimestamp("created")
                );
                Bug.addTags(cxn, bug);
                bugs.add(bug);
            }
        }
        logger.info("Retrieved {} Bugs", bugs.size());
        return bugs;
    }

    /*
     * Returns list of Bugs sorted by status
     */
    public static List<Bug> bugListStatus(PoolingDataSource pool, String statusStr) throws SQLException {
        List<Bug> bugs = new ArrayList<>();
        String bugSelectString = "SELECT bug_id, creator_id, assignee_id, " +
                "milestone_id, title, details, status, closed, created " +
                "FROM bug " +
                "WHERE (status = ? )" +
                "ORDER BY created ASC";

        try (Connection cxn = pool.getConnection();
            PreparedStatement ps = cxn.prepareStatement(bugSelectString)) {
            ps.setString(1, statusStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bug bug = new Bug(
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getInt("assignee_id"),
                            rs.getInt("milestone_id"),
                            rs.getString("title"),
                            rs.getString("details"),
                            rs.getString("status"),
                            rs.getTimestamp("closed"),
                            rs.getTimestamp("created")
                    );
                    Bug.addTags(cxn, bug);
                    bugs.add(bug);
                }
            }
        }
        logger.info("Retrieved {} Bugs by Status", bugs.size());
        return bugs;
    }

    /*
     * Returns list of Bugs sorted by Tag
     */
    public static List<Bug> bugListTag(PoolingDataSource pool, long tagId) throws SQLException {
        List<Bug> bugs = new ArrayList<>();
        String bugSelectString = "SELECT bug_id, creator_id, assignee_id, " +
                "milestone_id, title, details, status, closed, created " +
                "FROM bug JOIN bugtag USING (bug_id) " +
                "WHERE (tag_id = ? )" +
                "ORDER BY created ASC";

        try (Connection cxn = pool.getConnection();
             Statement stmt = cxn.createStatement();
             PreparedStatement ps = cxn.prepareStatement(bugSelectString)) {
            ps.setLong(1, tagId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bug bug = new Bug(
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getInt("assignee_id"),
                            rs.getInt("milestone_id"),
                            rs.getString("title"),
                            rs.getString("details"),
                            rs.getString("status"),
                            rs.getTimestamp("closed"),
                            rs.getTimestamp("created")
                    );
                    Bug.addTags(cxn, bug);
                    bugs.add(bug);
                }
            }
        }
        logger.info("Retrieved {} Bugs by Status", bugs.size());
        return bugs;
    }

    /*
     * Returns list of Bugs sorted by creator
     */
    public static List<Bug> retrieveBugsCreatedByUser(PoolingDataSource pool, long creator_id) throws SQLException, ParseException {

        String creatorSelectString =
                "SELECT * " +
                        "FROM bug " +
                        "WHERE creator_id = ?";

        List<Bug> bugs = new ArrayList<>();

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(creatorSelectString)) {
            ps.setLong(1, creator_id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bug bug = new Bug(
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getInt("assignee_id"),
                            rs.getInt("milestone_id"),
                            rs.getString("title"),
                            rs.getString("details"),
                            rs.getString("status"),
                            rs.getTimestamp("closed"),
                            rs.getTimestamp("created")
                    );
                    Bug.addTags(cxn, bug);
                    bugs.add(bug);
                }
            }
        }
        return bugs;
    }

    /*
     * Retrieve a Bug by it's Bug_ID.
     */
    public static Bug retrieveBug(PoolingDataSource pool, long bugId) throws SQLException {
        String bugSelectString = "SELECT bug_id, creator_id, assignee_id, milestone_id, title, details, status, closed, created " +
                "FROM bug " +
                "WHERE ( bug_id = ?) " +
                "ORDER BY created ASC";

        Bug bug = null;
        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(bugSelectString)) {
            ps.setLong(1, bugId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bug = new Bug(
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getInt("assignee_id"),
                            rs.getInt("milestone_id"),
                            rs.getString("title"),
                            rs.getString("details"),
                            rs.getString("status"),
                            rs.getTimestamp("closed"),
                            rs.getTimestamp("created")
                    );
                    Bug.addTags(cxn, bug);
                }
            }
        }
        return bug;
    }

    public static User bugCreaterName(PoolingDataSource pool, long bugID) throws SQLException {
        String creatorSelect = "SELECT * \n" +
                "FROM j_user \n" +
                "  JOIN bug ON (creator_id = user_id )\n" +
                "WHERE (bug_id = ?)";

        User user = null;
        try (Connection cxn = pool.getConnection()) {

            try (PreparedStatement ps = cxn.prepareStatement(creatorSelect)) {
                ps.setLong(1, bugID);

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
        }
        return user;
    }

    public static User bugAssigneeName(PoolingDataSource pool, long bugID) throws SQLException {
        String assigneeSelect = "SELECT * \n" +
                "FROM j_user \n" +
                "  JOIN bug ON (assignee_id = user_id )\n" +
                "WHERE (bug_id = ?)";
        User user = null;

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugID);

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
        }
        return user;
    }

    public static Milestone bugMilestoneName(PoolingDataSource pool, long bugID) throws SQLException {
        String msSelect = "SELECT * \n" +
                "FROM milestone \n" +
                "  JOIN bug ON (bug.milestone_id = milestone.milestone_id )\n" +
                "WHERE (bug_id = ?)";

        Milestone ms = null;
        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(msSelect)) {
                ps.setLong(1, bugID);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ms = new Milestone(
                                rs.getInt("milestone_id"),
                                rs.getString("name"),
                                rs.getDate("due_date"),
                                rs.getTimestamp("created")
                        );
                    }
                }
            }
        }

        return ms;

    }

    public static void addTags(Connection cxn, Bug bug) throws SQLException {
        String tagSelectString = "SELECT tag_id, title\n" +
                "FROM tag\n" +
                "    JOIN bugtag USING(tag_id)\n" +
                "    WHERE bug_id = ?;";

        PreparedStatement tps = cxn.prepareStatement(tagSelectString);
        tps.setLong(1, bug.getBugId());

        try (ResultSet trs = tps.executeQuery()) {
            while (trs.next()) {
                bug.getTags().add(new Tag(trs.getInt("tag_id"), trs.getString("title")));
            }
        }
    }

    /*
     * A list of bugs the user is subscribed to based on the TAGS they are subscribed to.
     * This is used in the user feed.
     */
    public static List<Bug> getBugTagSubList(PoolingDataSource pool, long userId) throws SQLException{
        List<Bug> bugs = new ArrayList<>();
        String bugTagSelect = "SELECT DISTINCT bug.*, tag.title AS tag " +
                "FROM bugtag " +
                "  JOIN bug ON (bugtag.bug_id = bug.bug_id) " +
                "  JOIN tagsubscription ON (bugtag.tag_id = tagsubscription.tag_id) " +
                "  JOIN tag ON (bugtag.tag_id = tag.tag_id) " +
                "  WHERE (tagsubscription.subscriber_id = ?) " +
                "ORDER BY tag ";

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(bugTagSelect)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bug bug = new Bug(
                            rs.getInt("bug_id"),
                            rs.getInt("creator_id"),
                            rs.getInt("assignee_id"),
                            rs.getInt("milestone_id"),
                            rs.getString("title"),
                            rs.getString("tag"),
                            rs.getString("status"),
                            rs.getTimestamp("closed"),
                            rs.getTimestamp("created")
                    );
                    bugs.add(bug);
                }
            }
        }
        return bugs;
    }

    /*
     * Returns a list of Bugs that a user is subscribed to filtered by a specific status or "all" for any status.
     */
    public static List<Bug> getBugSubStatusList(PoolingDataSource pool, long userId, String status) throws SQLException {
        List<Bug> bugs = new ArrayList<>();

        String userSelectString = "SELECT * " +
                "FROM bugsubscription " +
                "  JOIN bug USING (bug_id) " +
                "  JOIN j_user ON (subscriber_id = user_id) " +
                "WHERE (subscriber_id = ? AND status = ?)";

        if (status.equals("all")) {
            userSelectString = "SELECT * " +
                    "FROM bugsubscription " +
                    "  JOIN bug USING (bug_id) " +
                    "  JOIN j_user ON (subscriber_id = user_id) " +
                    "WHERE (subscriber_id = ?)";

            try (Connection cxn = pool.getConnection();
                 PreparedStatement ps = cxn.prepareStatement(userSelectString)) {
                ps.setLong(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Bug bug = new Bug(
                                rs.getInt("bug_id"),
                                rs.getInt("creator_id"),
                                rs.getInt("assignee_id"),
                                rs.getInt("milestone_id"),
                                rs.getString("title"),
                                rs.getString("details"),
                                rs.getString("status"),
                                rs.getTimestamp("closed"),
                                rs.getTimestamp("created")
                        );
                        Bug.addTags(cxn, bug);
                        bugs.add(bug);
                    }
                }
            }
        } else {
            try (Connection cxn = pool.getConnection();
                 PreparedStatement ps = cxn.prepareStatement(userSelectString)) {
                ps.setLong(1, userId);
                ps.setString(2, status);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Bug bug = new Bug(
                                rs.getInt("bug_id"),
                                rs.getInt("creator_id"),
                                rs.getInt("assignee_id"),
                                rs.getInt("milestone_id"),
                                rs.getString("title"),
                                rs.getString("details"),
                                rs.getString("status"),
                                rs.getTimestamp("closed"),
                                rs.getTimestamp("created")
                        );
                        Bug.addTags(cxn, bug);
                        bugs.add(bug);
                    }
                }
            }
        }


        return bugs;
    }
    public static void untagBug(PoolingDataSource pool, long bugId, long tagId) throws SQLException {
        String assigneeSelect = "DELETE FROM bugtag WHERE bug_id = ? AND tag_id = ?";

        try (Connection cxn = pool.getConnection()) {
            try (PreparedStatement ps = cxn.prepareStatement(assigneeSelect)) {
                ps.setLong(1, bugId);
                ps.setLong(2, tagId);
                ps.execute();
            }
        }

    }

    /*
     *  tag a bug
     */
    public static void tagBug(PoolingDataSource pool, long bugId, long tagId) throws SQLException {
        //first make sure it's not already tagged!
        String bugSelect = "SELECT * FROM bugtag WHERE(bug_id = ? AND tag_id = ?)";

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(bugSelect)) {
            ps.setLong(1, bugId);
            ps.setLong(2, tagId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String bugTagInsert = "INSERT INTO bugtag(bug_id, tag_id) VALUES (?,?)";

        try (Connection cxn = pool.getConnection();
             PreparedStatement ps = cxn.prepareStatement(bugTagInsert)) {
            ps.setLong(1, bugId);
            ps.setLong(2, tagId);
            ps.execute();
        }
    }
}
