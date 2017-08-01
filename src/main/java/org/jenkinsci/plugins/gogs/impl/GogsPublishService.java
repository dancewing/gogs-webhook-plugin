package org.jenkinsci.plugins.gogs.impl;

import jenkins.plugins.hipchat.model.notifications.Notification;
import org.jenkinsci.plugins.gogs.HipChatService;
import org.jenkinsci.plugins.gogs.exceptions.NotificationException;

/**
 * Created by jeff on 01/08/2017.
 */
public class GogsPublishService extends HipChatService {
    @Override
    public void publish(Notification notification) throws NotificationException {

    }
}
