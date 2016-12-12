package edu.boisestate.cs410.jeara.web;

import edu.boisestate.cs410.jeara.web.models.*;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.Request;
import spark.template.pebble.PebbleTemplateEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Server for the jeara database.
 */
public class JearaServer {
    private static final Logger logger = LoggerFactory.getLogger(JearaServer.class);

    private final PoolingDataSource<? extends Connection> pool;
    private final Service http;
    private final TemplateEngine engine;

    public JearaServer (PoolingDataSource<? extends Connection> pds, Service svc) {
        pool = pds;
        http = svc;
        engine = new PebbleTemplateEngine(new ClasspathLoader());

        http.get("/", this::rootPage, engine);

        // Bug Views
        http.get("/bugs", this::redirectToFolder);
        http.get("/bugs/", this::bugIndexPage, engine);
        http.get("/bugs/:bug_id", this::redirectToFolder);
        http.get("/bugs/:bug_id/", this::bugShowPage, engine);
        http.get("/assignee", this::redirectToFolder);
        http.get("/assignee/", this::bugAssigneeIndexPage, engine);
        http.get("/assignee/:assignee_id", this::redirectToFolder);
        http.get("/assignee/:assignee_id/", this::bugAssigneeShowPage, engine);
        http.get("/creator", this::redirectToFolder);
        http.get("/creator/", this::bugCreatorIndexPage, engine);
        http.get("/creator/:creator_id", this::redirectToFolder);
        http.get("/creator/:creator_id/", this::bugCreatorShowPage, engine);
        http.get("/status", this::redirectToFolder);
        http.get("/status/", this::bugStatusIndexPage, engine);
        http.get("/status/:status", this::redirectToFolder);
        http.get("/status/:status/", this::bugStatusShowPage, engine);
        http.get("/bug/new", this::redirectToFolder);
        http.get("/bug/new/", this::newBug, engine);
        http.post("/bug/create", this::redirectToFolder);
        http.post("/bug/create/", this::createBug);

        http.post("/comment/create", this::redirectToFolder);
        http.post("/comment/create/", this::createComment);

        // Subscriptions
        http.post("/bugsubscription/create", this::redirectToFolder);
        http.post("/bugsubscription/create/", this::createBugSubscription);
        http.post("/bugsubscription/destroy", this::redirectToFolder);
        http.post("/bugsubscription/destroy/", this::destroyBugSubscription);

        http.post("/tagsubscription/create", this::redirectToFolder);
        http.post("/tagsubscription/create/", this::createTagSubscription);
        http.post("/tagsubscription/destroy", this::redirectToFolder);
        http.post("/tagsubscription/destroy/", this::destroyTagSubscription);

        http.get("/bug/edit/:bug_id", this::redirectToFolder);
        http.get("/bug/edit/:bug_id/", this::editBug, engine);
        http.post("/bug/update", this::redirectToFolder);
        http.post("/bug/update/", this::bugUpdate);


        // User Views
        http.get("/users", this::redirectToFolder);
        http.get("/users/", this::userIndexPage, engine);
        http.get("/users/:user_id", this::redirectToFolder);
        http.get("/users/:user_id/", this::userShowPage, engine);
        http.get("/users/:user_id/feed", this::redirectToFolder);
        http.get("/users/:user_id/feed/", this::userFeedPage, engine);
        http.get("/logout", this::redirectToFolder);
        http.get("/logout/", this::logout);
        http.get("/create", this::redirectToFolder);
        http.post("/create/", this::create);
        http.post("/login", this::redirectToFolder);
        http.post("/login/", this::login);

        // Milestone Views
        http.get("/milestones", this::redirectToFolder);
        http.get("/milestones/", this::milestoneIndexPage, engine);
        http.get("/milestones/new", this::redirectToFolder);
        http.get("/milestones/new/", this::newMilestone, engine);
        http.post("/milestones/create", this::createMilestone);
        http.post("/milestones/create/", this::createMilestone);
        http.post("/milestones/remove", this::redirectToFolder);
        http.post("/milestones/remove/", this::removeMilestone);
        http.get("/milestones/:milestone_id", this::redirectToFolder);
        http.get("/milestones/:milestone_id/", this::milestoneShowPage, engine);

        // Tag Views
        http.get("/tags", this::redirectToFolder);
        http.get("/tags/", this::tagIndexPage, engine);
        http.get("/tags/:tag_id", this::redirectToFolder);
        http.get("/tags/:tag_id/", this::tagShowPage, engine);
        http.get("/tag/new", this::redirectToFolder);
        http.get("/tag/new/", this::newTag, engine);
        http.post("/newtag/create", this::createTag);
        http.post("/newtag/create/", this::createTag);
        http.post("/tags/untag", this::createTag);
        http.post("/tags/untag/", this::untag);
        http.post("/bugtag", this::bugtag);
        http.post("/bugtag/", this::bugtag);


        //search
        http.get("/search", this::search, engine);
    }

    ModelAndView rootPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        fields.put("idx", true);
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
            response.redirect("users/" + userId, 303);
        }

        return new ModelAndView(fields, "home.html.twig");
    }

    public ModelAndView search(Request request, Response response) throws SQLException {
        Map<String, Object> fields = new HashMap<>();

        String query = request.queryParams("q");
        fields.put("query", query);

        Long userId = request.session().attribute("userId");
        if(userId == null) {
            http.halt(400, "You must be signed in to search");
        }

        fields.put("userId", userId);
        fields.put("user", User.retrieveUser(pool,userId));

        String searchQuery= "SELECT bug_id, title, details, status\n" +
                "FROM bug JOIN bug_search USING (bug_id),\n" +
                " plainto_tsquery(?) query\n" +
                "WHERE bug_vector @@ query\n" +
                "ORDER BY ts_rank(bug_vector, query) DESC\n" +
                "LIMIT 50";

        if(query != null) {
            try (Connection cxn = pool.getConnection();
                 PreparedStatement ps = cxn.prepareStatement(searchQuery)) {
                ps.setString(1, query);
                List<Map<String, Object>> results = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> res = new HashMap<>();
                        res.put("bugId", rs.getInt("bug_id"));
                        res.put("title", rs.getString("title"));
                        res.put("details", rs.getString("details"));
                        res.put("status", rs.getString("status"));
                        results.add(res);
                    }
                }
                fields.put("results", results);
            }
        }

        return new ModelAndView(fields, "search.html.twig");
    }

    ModelAndView newBug (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }  else {
            http.halt(403, "user not logged in");
        }


        initializeCSRFToken(request, fields);

        return new ModelAndView(fields, "bugs/new.html.twig");
    }

    private Object createBug (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        Map<String,Object> fields = new HashMap<>();

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        String btitle = request.queryParams("title");
        if (btitle == null || btitle.isEmpty()) {
            http.halt(400, "bad data");
        }

        String bdetails = request.queryParams("details");
        if (bdetails == null || bdetails.isEmpty()) {
            http.halt(400, "bad data");
        }

        long bcreator = Integer.parseInt(request.queryParams("creator_id"));
        long bugId = Bug.create(request,pool,http);

        BugSubscription.createBugSubscription(pool, bugId, bcreator);

        response.redirect("/bugs/" + bugId, 303);

        return "new bug made, B";
    }

    ModelAndView editBug (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        long bug_id = Integer.parseInt(request.params("bug_id"));
        Long userId = request.session().attribute("userId");
        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        } else {
            return new ModelAndView(fields, "home.html.twig");
        }

        fields.put("Bug", Bug.retrieveBug(pool, bug_id));
        fields.put("users", User.retrieveUsers(pool));
        fields.put("Assignee",  Bug.bugAssigneeName(pool, bug_id));
        fields.put("Milestone", Bug.bugMilestoneName(pool,bug_id));

        try {
            fields.put("milestones", Milestone.retrieveMilestones(pool));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        initializeCSRFToken(request, fields);

        return new ModelAndView(fields, "bugs/edit.html.twig");
    }

    private Object bugUpdate (Request request, Response response) throws SQLException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        Map<String,Object> fields = new HashMap<>();

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        String btitle = request.queryParams("title");
        if (btitle == null || btitle.isEmpty()) {
            http.halt(400, "Title cannot be empty");
        }

        String bdetails = request.queryParams("details");
        if (bdetails == null || bdetails.isEmpty()) {
            http.halt(400, "Details cannot be empty");
        }

        long bugId = Integer.parseInt(request.queryParams("bug_id"));

        //create bug sub.
        long assID = Integer.parseInt(request.queryParams("assignee_id"));

        //must check if user is already subscribed first...
        if (assID > 0){
            if(!BugSubscription.isBugSubscribed(pool,bugId, assID)){
                BugSubscription.createBugSubscription(pool,bugId,assID);
            }
        }else if (assID == 0){
            if(BugSubscription.isBugSubscribed(pool,bugId, assID)){
                BugSubscription.destroyBugSubscription(pool,bugId,assID);
            }
        }

        //edit the bug
        bugId = Bug.edit(request,pool,http);

        response.redirect("/bugs/" + bugId, 303);

        return "edited a bug";
    }

    ModelAndView bugIndexPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool, userId));
        } else {
            return new ModelAndView(fields, "home.html.twig");
        }

        fields.put("Bugs", Bug.bugList(pool));

        return new ModelAndView(fields, "bugs/index.html.twig");
    }

    ModelAndView bugShowPage (Request request, Response response) throws SQLException {
        long bug_id = Integer.parseInt(request.params("bug_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }else{
            http.halt(403, "user not logged in");
        }

        fields.put("Bug", Bug.retrieveBug(pool, bug_id));
        fields.put("Assignee", Bug.bugAssigneeName(pool, bug_id));
        fields.put("Creator", Bug.bugCreaterName(pool, bug_id));
        fields.put("Milestone", Bug.bugMilestoneName(pool, bug_id));
        fields.put("userBugSubscription", BugSubscription.getBugSubscription(pool, bug_id, userId));
        fields.put("Comments", Comment.retrieveComments(pool,bug_id));
        fields.put("Tags", Tag.retrieveTags(pool));

        initializeCSRFToken(request, fields);

        return new ModelAndView(fields, "bugs/show.html.twig");
    }


    ModelAndView bugAssigneeIndexPage (Request request, Response response) throws SQLException {
        //long bug_id = Integer.parseInt(request.params("bug_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");

        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }

        initializeCSRFToken(request, fields);

        fields.put("users", User.retrieveUsers(pool));

        return new ModelAndView(fields, "bugs/assigneeindex.html.twig");
    }

    ModelAndView bugAssigneeShowPage (Request request, Response response) throws SQLException {
        long assignee_id = Integer.parseInt(request.params("assignee_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }

        fields.put("Assignee", User.retrieveUser(pool,assignee_id));
        try {
            fields.put("Bugs", Bug.retrieveBugsAssignedToUser(pool,assignee_id));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return new ModelAndView(fields, "bugs/assigneeshow.html.twig");
    }

    ModelAndView bugCreatorIndexPage (Request request, Response response) throws SQLException {
        //long bug_id = Integer.parseInt(request.params("bug_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }else {
            http.halt(403, "user not logged in");
        }

        fields.put("users", User.retrieveUsers(pool));

        return new ModelAndView(fields, "bugs/creatorindex.html.twig");
    }
    ModelAndView bugCreatorShowPage (Request request, Response response) throws SQLException {
        long creator_id = Integer.parseInt(request.params("creator_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        } else {
            http.halt(403, "user not logged in");
        }

        fields.put("Creator", User.retrieveUser(pool,creator_id));
        try {
            fields.put("Bugs", Bug.retrieveBugsCreatedByUser(pool, creator_id));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return new ModelAndView(fields, "bugs/creatorshow.html.twig");
    }

    ModelAndView bugStatusIndexPage (Request request, Response response) throws SQLException {
       // long bug_id = Integer.parseInt(request.params("bug_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        } else{
            http.halt(403, "user not logged in");
        }

        return new ModelAndView(fields, "bugs/statusindex.html.twig");
    }
    ModelAndView bugStatusShowPage (Request request, Response response) throws SQLException {
        String statusStr = request.params("status");
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        } else{
            http.halt(403, "user not logged in");
        }

        fields.put("Status", statusStr);
        fields.put("Bugs", Bug.bugListStatus(pool, statusStr));

        return new ModelAndView(fields, "bugs/statusshow.html.twig");
    }

    private Object createComment (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        Map<String,Object> fields = new HashMap<>();

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long commieCreator = Integer.parseInt(request.queryParams("creator_id"));
        long bugId = Integer.parseInt(request.queryParams("bug_id"));

        String commieBody = request.queryParams("body");
        if (commieBody == null || commieBody.isEmpty()) {
            http.halt(400, "Bad Comment.");
        }

        long comId = Comment.create(request,pool,http);

        //create bug sub.
        //must check if user is already subscribed first...
        if(!BugSubscription.isBugSubscribed(pool,bugId, commieCreator)){
            BugSubscription.createBugSubscription(pool,bugId,commieCreator);
        }

        response.redirect("/bugs/" + bugId, 303);

        return "new commie made, Comrade";
    }

    String create (Request request, Response response) throws SQLException {

        long userId = User.create(request, pool, http);

        Session s = request.session(true);
        s.attribute("userId", userId);

        response.redirect("/", 303);
        return "";
    }
    String login (Request request, Response response) throws SQLException {
        long userId = User.login(request, pool, http);
        if(userId == 0) {
            http.halt(400, "Invalid username or password");
        }

        response.redirect("/", 303);
        return "";
    }
    String logout (Request request, Response response) {
        User.logout(request);
        response.redirect("/", 303);
        return "";
    }

    ModelAndView userIndexPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if (userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool, userId));
        }

        fields.put("users", User.retrieveUsers(pool));

        return new ModelAndView(fields, "users/index.html.twig");
    }
    ModelAndView userShowPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        int userShowId = Integer.parseInt(request.params("user_id"));

        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool, userId));
            fields.put("showUser", User.retrieveUser(pool, userShowId));
            try {
                fields.put("userBugs", Bug.retrieveBugsAssignedToUser(pool, userShowId));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            http.halt(403, "user not logged in");
        }

        return new ModelAndView(fields, "users/show.html.twig");
    }

    ModelAndView userFeedPage (Request request, Response response) throws SQLException, ParseException {
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool, userId));
            fields.put("assignedBugs", Bug.retrieveBugsAssignedToUser(pool,userId));
            fields.put("subscribedBugs", Bug.getBugSubStatusList(pool,userId, "all"));
            fields.put("subscribedBugsOpen", Bug.getBugSubStatusList(pool,userId, "open"));
            fields.put("subscribedBugsClosed", Bug.getBugSubStatusList(pool,userId, "closed"));
            fields.put("subscribedBugsRej", Bug.getBugSubStatusList(pool,userId, "rejected"));
            fields.put("taggedBugs", Bug.getBugTagSubList(pool, userId));

        } else {
            http.halt(403, "user not logged in");
        }

        return new ModelAndView(fields, "users/feed.html.twig");
    }

    ModelAndView tagIndexPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }

        try (Connection cxn = pool.getConnection()) {
            fields.put("Tags", Tag.retrieveTags(pool));
        }

        return new ModelAndView(fields, "tags/index.html.twig");
    }

    ModelAndView tagShowPage (Request request, Response response) throws SQLException {
        long tag_id = Integer.parseInt(request.params("tag_id"));
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }

        initializeCSRFToken(request, fields);

        fields.put("Tag", Tag.getTag(pool,tag_id));
        fields.put("Bugs", Bug.bugListTag(pool, tag_id));
        fields.put("userTagSubscription", TagSubscription.getTagSubscription(pool, tag_id, userId));

        return new ModelAndView(fields, "tags/show.html.twig");
    }

    ModelAndView newTag (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }  else {
            http.halt(403, "user not logged in");
        }

        initializeCSRFToken(request, fields);

        return new ModelAndView(fields, "tags/new.html.twig");
    }

    private Object createTag (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        Map<String,Object> fields = new HashMap<>();

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        String btitle = request.queryParams("title");
        if (btitle == null || btitle.isEmpty()) {
            http.halt(400, "bad data");
        }

        long tagId = Tag.create(request, pool, http);

        response.redirect("/tags/" + tagId, 303);

        return "new Tag made, G.";
    }

    ModelAndView milestoneIndexPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }

        try {
            fields.put("milestones", Milestone.retrieveMilestones(pool));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return new ModelAndView(fields, "milestones/index.html.twig");
    }

    ModelAndView newMilestone (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();

        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }  else {
            http.halt(403, "user not logged in");
        }

        initializeCSRFToken(request, fields);

        return new ModelAndView(fields, "milestones/new.html.twig");
    }

    private Object createMilestone (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        Map<String,Object> fields = new HashMap<>();

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        String dueDateStr = request.queryParams("due_date");
        if (dueDateStr == null || dueDateStr.isEmpty()) {
            http.halt(400, "bad data");
        }
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date dueDate  = formatter.parse(dueDateStr);

        String name = request.queryParams("name");
        if (name == null || name.isEmpty()) {
            http.halt(400, "bad data");
        }

        int milestoneId = Milestone.createMilestone(pool, name, new java.sql.Date(dueDate.getTime()));

        response.redirect("/milestones/" + milestoneId, 303);
        return "Created New Milestone";
    }

    ModelAndView milestoneShowPage (Request request, Response response) throws SQLException {
        Map<String,Object> fields = new HashMap<>();
        //get user id from session and put in fields so user has access to pages
        Long userId = request.session().attribute("userId");
        if(userId != null) {
            fields.put("userId", userId);
            fields.put("user", User.retrieveUser(pool,userId));
        }

        int milestoneId = Integer.parseInt(request.params("milestone_id"));

        try {
            fields.put("milestone", Milestone.retrieveMilestone(pool, milestoneId));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        initializeCSRFToken(request, fields);

        return new ModelAndView(fields, "milestones/show.html.twig");
    }

    public String redirectToFolder(Request request, Response response) {
        String path = request.pathInfo();
        response.redirect(path + "/", 301);
        return "Redirecting to " + path + "/";
    }

    private Object createBugSubscription (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long userId = request.session().attribute("userId");
        long bugId = Integer.parseInt(request.queryParams("bug_id"));

        BugSubscription.createBugSubscription(pool, bugId, userId);

        response.redirect("/bugs/" + bugId, 303);

        return "new bug made, B";
    }

    private Object destroyBugSubscription (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long userId = request.session().attribute("userId");
        long bugId = Integer.parseInt(request.queryParams("bug_id"));

        BugSubscription.destroyBugSubscription(pool, bugId, userId);

        response.redirect("/bugs/" + bugId, 303);

        return "unsubscribed from bug";
    }

    private Object createTagSubscription (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long userId = request.session().attribute("userId");
        long tagId = Integer.parseInt(request.queryParams("tag_id"));

        TagSubscription.createTagSubscription(pool, tagId, userId);

        response.redirect("/tags/" + tagId, 303);

        return "subscribed to bug";
    }

    private Object destroyTagSubscription (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long userId = request.session().attribute("userId");
        long tagId = Integer.parseInt(request.queryParams("tag_id"));

        TagSubscription.destroyTagSubscription(pool, tagId, userId);

        response.redirect("/tags/" + tagId, 303);

        return "new tag sub destroyed, B";
    }

    private Object bugtag (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long bugId = Integer.parseInt(request.queryParams("bug_id"));
        long tagId = Integer.parseInt(request.queryParams("tag_id"));

        Bug.tagBug(pool, bugId, tagId);

        response.redirect("/bugs/" + bugId, 303);

        return "new bugtag";
    }
    private Object untag (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long bugId = Integer.parseInt(request.queryParams("bug_id"));
        long tagId = Integer.parseInt(request.queryParams("tag_id"));

        Bug.untagBug(pool, bugId, tagId);

        response.redirect("/tags/" + tagId, 303);

        return "new bug made, B";
    }

    private Object removeMilestone (Request request, Response response) throws SQLException, ParseException {
        if (null == request.session().attribute("userId")) {
            http.halt(403, "user not logged in");
        }

        String token = request.session().attribute("csrf_token");
        String submittedToken = request.queryParams("csrf_token");
        if (token == null || !token.equals(submittedToken)) {
            http.halt(400, "invalid CSRF token");
        }

        long bugId = Integer.parseInt(request.queryParams("bug_id"));
        long milestoneId = Integer.parseInt(request.queryParams("milestone_id"));

        Milestone.removeBugFromMilestone(pool, bugId, milestoneId);

        response.redirect("/milestones/" + milestoneId, 303);

        return "bug removed from milestone";
    }

    private void initializeCSRFToken (Request request, Map<String,Object> fields) {
        String token = request.session().attribute("csrf_token");
        if (token == null) {
            SecureRandom rng = new SecureRandom();
            byte[] bytes = new byte[8];
            rng.nextBytes(bytes);
            token = Base64.getEncoder().encodeToString(bytes);
            request.session(true).attribute("csrf_token", token);
        }
        fields.put("csrf_token", token);
    }
}
