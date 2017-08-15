package org.jenkinsci.plugins.gogs;

/**
 * Created by jeff on 14/08/2017.
 */
public class CauseData {

    private String deliveryID = "";
    private String callback = "";

    public String getDeliveryID() {
        return deliveryID;
    }

    public void setDeliveryID(String deliveryID) {
        this.deliveryID = deliveryID;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }
}
