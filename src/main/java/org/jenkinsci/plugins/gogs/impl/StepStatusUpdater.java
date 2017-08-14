package org.jenkinsci.plugins.gogs.impl;

import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.logging.Level;
import org.jenkinsci.plugins.gogs.GogsCause;
import org.jenkinsci.plugins.gogs.GogsProjectProperty;
import org.jenkinsci.plugins.gogs.PublishService;
import org.jenkinsci.plugins.gogs.exceptions.NotificationException;
import org.jenkinsci.plugins.gogs.model.BuildState;
import org.jenkinsci.plugins.gogs.model.notifications.Notification;

import java.util.logging.Logger;

public class StepStatusUpdater {

    private final static Logger LOGGER = Logger.getLogger(StepStatusUpdater.class.getName());

    public static void updateStepStatus(Run<?, ?> build, TaskListener listener, BuildState state, String name) {
        GogsCause cause =  build.getCause(GogsCause.class);



        if (cause!=null) {
            Notification notification = new Notification();
            notification.setState(state.name());
            notification.setStep(name);

            notification.setDeliveryID(cause.getDeliveryID());
            notification.setNumber(build.getNumber());
            if (state == BuildState.success || state == BuildState.failed) {
                notification.setTimeInMillis(build.getTimeInMillis());
            }

            if (state == BuildState.running) {
                notification.setStartTimeInMillis(build.getStartTimeInMillis());
            }

            PublishService publishService = new GogsPublishService();

            GogsProjectProperty projectProperty =  build.getParent().getProperty(GogsProjectProperty.class);

            String signature = "";

            if (projectProperty!=null) {
                signature = projectProperty.getGogsSecret();
            }

            try {
                publishService.publish(cause.getCallback(),signature,  notification);
            } catch (NotificationException e) {
                LOGGER.log(Level.INFO, "fail to publish notification");
            }

        } else {
            LOGGER.log(Level.INFO, "Job not start by gogs");
        }
    }


    public static class GitLabBranchBuild {
        private final String projectId;
        private final String revisionHash;

        public GitLabBranchBuild(final String projectId, final String revisionHash) {
            this.projectId = projectId;
            this.revisionHash = revisionHash;
        }

        public String getProjectId() {
            return this.projectId;
        }

        public String getRevisionHash() {
            return this.revisionHash;
        }
    }
}
