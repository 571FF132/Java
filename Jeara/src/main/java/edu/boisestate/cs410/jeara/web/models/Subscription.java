package edu.boisestate.cs410.jeara.web.models;

/**
 * Created by benneely on 11/7/16.
 */
public class Subscription {
    int subscriptionId;

    public Subscription(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "subscriptionId=" + subscriptionId +
                '}';
    }
}
