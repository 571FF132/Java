package edu.boisestate.cs410.jeara.web.models;

/**
 * Created by benneely on 11/7/16.
 */
public class BugTag {
    int bug_id;
    int tag_id;

    public BugTag(int bug_id, int tag_id) {
        this.bug_id = bug_id;
        this.tag_id = tag_id;
    }

    public int getBug_id() {
        return bug_id;
    }

    public int getTag_id() {
        return tag_id;
    }

    @Override
    public String toString() {
        return "BugTag{" +
                "bug_id=" + bug_id +
                ", tag_id=" + tag_id +
                '}';
    }
}
