DROP TABLE IF EXISTS j_user CASCADE;
DROP TABLE IF EXISTS milestone CASCADE;
DROP TABLE IF EXISTS bug CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS tag CASCADE;
DROP TABLE IF EXISTS subscription CASCADE;
DROP TABLE IF EXISTS bugtag CASCADE;
DROP TABLE IF EXISTS tagsubscription CASCADE;
DROP TABLE IF EXISTS bugsubscription CASCADE;


CREATE TABLE j_user (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(32) NOT NULL UNIQUE,
    pw_hash VARCHAR(100) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE milestone (
    milestone_id SERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    due_date DATE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bug (
    bug_id SERIAL PRIMARY KEY,
    creator_id INTEGER REFERENCES j_user (user_id),
    assignee_id INTEGER REFERENCES j_user (user_id),
    milestone_id INTEGER REFERENCES milestone (milestone_id),
    title VARCHAR(128) NOT NULL,
    details TEXT,
    status VARCHAR,
    closed TIMESTAMP,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comment (
    comment_id SERIAL PRIMARY KEY,
    bug_id INTEGER REFERENCES bug (bug_id),
    creator_id INTEGER REFERENCES j_user (user_id),
    body VARCHAR,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE tag (
    tag_id SERIAL PRIMARY KEY,
    title VARCHAR(128) NOT NULL
);


CREATE TABLE bugtag (
    bug_id INTEGER REFERENCES bug (bug_id),
    tag_id INTEGER REFERENCES tag (tag_id),
    PRIMARY KEY(bug_id, tag_id)
);

CREATE TABLE tagsubscription (
    tag_id INTEGER REFERENCES tag (tag_id),
    subscriber_id INTEGER REFERENCES j_user (user_id),
    PRIMARY KEY(tag_id, subscriber_id)
);

CREATE TABLE bugsubscription (
    bug_id INTEGER REFERENCES bug (bug_id),
    subscriber_id INTEGER REFERENCES j_user (user_id),
    PRIMARY KEY(bug_id, subscriber_id)
);

