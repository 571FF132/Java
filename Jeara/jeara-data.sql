--all passwords are: password
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('george@example.com', 'George', 'george', '$2a$10$tGR4uF3bwxZxrZdkNjLXmOOWh/yezGtFRGejQ5MXjiI35jkpDkzaC', '2016-11-14 22:46:42.668255');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('bob@jeara.com', 'bob', 'bobbyboy', '$2a$10$t.x0FEhW0TYgChU7rqtLm.2c7oMJ1scEOOUaL5ynX47ACXDd.wnFm', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('joe@jeara.com', 'joe', 'joedirt', '$2a$10$cc7IjhfD5QxtleltuNvDJe9IraUZhXKkHoML7aXOym9.0MM5h0.iO', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('sally@jeara.com', 'sally', 'sallythatgirl', '$2a$10$7eelGahHbm08CNVCb/8CL.iuXC/eam3q3.LRkH55/dbor5sRBVNhS', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('susy@jeara.com', 'susy', 'susyq', '$2a$10$mG4MmUbcA880hvnwGEvIl.nndPr5P0pLnnJNArjypuLRhQ9vHKis2', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('nick@jeara.com', 'nick', 'saintnick', '$2a$10$WiEv0q.MK6JTLzcKSTz2y.nZub5vhADGsnpyFkOnOATwyC0bVfYMC', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('billy@jeara.com', 'billy', 'billygoat', '$2a$10$zKmdd.eHq6.vF1X.Ium71u0rF0vd0qPQH5QrABqFiCZf5kiznsXN6', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('mike@jeara.com', 'mike', 'mikeandike', '$2a$10$jV5mSQeoWL5Hvco1GNm3BOOuDbTVV.wNqX0LpKcmYTtLFHuWxo67y', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('mandy@jeara.com', 'mandy', 'mmandy', '$2a$10$4746/fYs4js6uWaWxsJrwO8j01cKlasUJ9kcybI6VzgwGtl/h6ekO', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('miranda@jeara.com', 'miranda', 'mmiranda', '$2a$10$ssSDMiKXbBC/byvn/EYDA.tGols4wiUjtIy5EHvDJqGwZCIlCcFLa', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('jordan@jeara.com', 'jordan', 'mj', '$2a$10$nex/u/N5YvuJ6J2I.qiNM.h57n37cdL6X5vCpVhST1qrxGurAgcbW', '2016-11-14 22:47:02.790532');
INSERT INTO j_user (email, name, username, pw_hash, created) VALUES ('tim@jeara.com', 'Tim', 'tim', '$2a$10$nMv6irCp4xi6p65sJfutZexxRGZe3Sy84Ig/IY0REKv/hsfIRkJ.e', '2016-11-14 22:50:48.513032');

INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (2,2,null,'Array has null pointer exception','There is a null pointer exception when I try to point to null in my array.','open',null,current_timestamp);
INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (2,3,null,'Bug in constructor', 'The constructor for bug object has a bug in it.  Pun intended.  Please remove the puns, too.','open', null, current_timestamp);

INSERT INTO milestone (name, due_date, created) VALUES ('configure', current_date,current_timestamp);
INSERT INTO milestone (name, due_date, created) VALUES ('assembly', current_date,current_timestamp);

INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (1,1,1,'font to large', 'size 72px is too big for the footer','closed',current_timestamp,current_timestamp);
INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (4,1,2,'font is too small', 'size 2px is too small for the <h1>','closed',current_timestamp,current_timestamp);
INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (1,2,1,'header needs to be centered', 'the header is too far right. please center it', 'open', null, current_timestamp);
INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (1,3,2, 'footer needs to be off-centered', 'the footer is perfectly centered. please make it go right', 'open', null, current_timestamp);
INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (3,4,null,'broken link in nav', 'the link to google takes you to gooogle.com','open',null,current_timestamp);
INSERT INTO bug (creator_id, assignee_id, milestone_id, title, details, status, closed, created) VALUES (1,null,1,'website down', 'the website is down','open',null,current_timestamp);

INSERT INTO comment (bug_id, creator_id, body, created) VALUES (1,1,'this should take a few days',current_timestamp);
INSERT INTO comment (bug_id, creator_id, body, created) VALUES (1,2,'4 or 5 days at least!',current_timestamp);
INSERT INTO comment (bug_id, creator_id, body, created) VALUES (1,3,'i could do it in 3 days', current_timestamp);
INSERT INTO comment (bug_id, creator_id, body, created) VALUES (3,1,'this might take a while',current_timestamp);
INSERT INTO comment (bug_id, creator_id, body, created) VALUES (8,2, 'let me get the CEO on the phone', current_timestamp);

INSERT INTO tag (title) VALUES ('Java');
INSERT INTO tag (title) VALUES ('SQL');
INSERT INTO tag (title) VALUES ('php');
INSERT INTO tag (title) VALUES ('Regex');
INSERT INTO tag (title) VALUES ('WebDev');

INSERT INTO bugsubscription (bug_id, subscriber_id) VALUES (1,1);
INSERT INTO bugsubscription (bug_id, subscriber_id) VALUES (1,2);
INSERT INTO bugsubscription (bug_id, subscriber_id) VALUES (1,3);
INSERT INTO bugsubscription (bug_id, subscriber_id) VALUES (2,2);
INSERT INTO bugsubscription (bug_id, subscriber_id) VALUES (2,1);
INSERT INTO bugsubscription (bug_id, subscriber_id) VALUES (3,2);

INSERT INTO bugtag (bug_id, tag_id) VALUES (1,1);
INSERT INTO bugtag (bug_id, tag_id) VALUES (2,1);
INSERT INTO bugtag (bug_id, tag_id) VALUES (1,2);
INSERT INTO bugtag (bug_id, tag_id) VALUES (3,5);
INSERT INTO bugtag (bug_id, tag_id) VALUES (4,5);

INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (5, 1);
INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (5, 2);
INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (5, 3);
INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (4, 1);
INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (3, 1);
INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (2, 1);
INSERT INTO tagsubscription (tag_id, subscriber_id) VALUES (1, 1);

--Set up bug search
DROP TABLE IF EXISTS bug_search;
CREATE TABLE bug_search (
  bug_id INTEGER PRIMARY KEY REFERENCES bug ON DELETE CASCADE,
  bug_vector TSVECTOR NOT NULL,
  current BOOLEAN NOT NULL
);

INSERT INTO bug_search (bug_id, bug_vector, current)
  SELECT bug_id,
    setweight(to_tsvector(title), 'A')
    || setweight(to_tsvector(coalesce(details, '')), 'B')
    || setweight(to_tsvector(coalesce(status, '')), 'C'),
    TRUE
  FROM bug;

CREATE INDEX bug_search_idx ON bug_search USING GIN(bug_vector);
ANALYZE bug_search;
