package org.jenkinsci.plugins.gogs.workflow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gogs.GogsCause;
import org.jenkinsci.plugins.gogs.GogsProjectProperty;
import org.jenkinsci.plugins.gogs.Messages;
import org.jenkinsci.plugins.gogs.PublishService;
import org.jenkinsci.plugins.gogs.exceptions.NotificationException;
import org.jenkinsci.plugins.gogs.impl.GogsPublishService;
import org.jenkinsci.plugins.gogs.model.notifications.Notification;
import org.jenkinsci.plugins.gogs.utils.BuildUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Workflow step to send a HipChat room notification.
 */
public class GogsSendStep extends AbstractStepImpl {

    private static final Logger logger = Logger.getLogger(GogsSendStep.class.getName());

    public final RunWrapper build;

    @DataBoundSetter
    public boolean failOnError;

    @DataBoundConstructor
    public GogsSendStep(@Nonnull RunWrapper currentBuild) {
        this.build = currentBuild;
    }

    @Extension
    public static class SenderDescriptorImpl extends AbstractStepDescriptorImpl {

        public SenderDescriptorImpl() {
            super(GogsSendStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "gogsSend";
        }

        @Override
        public String getDisplayName() {
            return Messages.HipChatSendStepDisplayName();
        }

    }

    public static class GogsSendStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient BuildUtils buildUtils;
        //        @Inject
//        private transient CredentialUtils credentialUtils;
        @Inject
        private transient GogsSendStep step;
        @StepContextParameter
        private transient TaskListener listener;
        @StepContextParameter
        private transient Run<?, ?> run;

        @Override
        protected Void run() throws Exception {

//            EnvVars environment = run.getEnvironment(listener);
//            if (environment!=null) {
//                Set<Map.Entry<String, String>> entries = environment.entrySet();
//                Iterator<Map.Entry<String, String>> iterator = entries.iterator();
//                while (iterator.hasNext()) {
//                    Map.Entry<String, String> next = iterator.next();
//                    logger.info("key :" + next.getKey() +", value: " + next.getValue());
//                }
//            }

            logger.info(run.getLogText().toString());

            GogsCause gogsCause = step.build.getRawBuild().getCause(GogsCause.class);

            if (gogsCause == null) {
                listener.error(Messages.NotificationFailed(""));
                return null;
            }

            String signature = "";
            GogsProjectProperty gogsProjectProperty = run.getParent().getProperty(GogsProjectProperty.class);
            if (gogsProjectProperty != null) {
                signature = gogsProjectProperty.getGogsSecret();
            }


            PublishService publishService = new GogsPublishService();
            try {
                publishService.publish(gogsCause.getCallback(), signature, new Notification().withResult(step.build.getResult())
                        .withDisplayName(step.build.getDisplayName())
                        .withDescription(step.build.getDescription())
                        .withNumber(step.build.getNumber())
                        .withTimeInMillis(step.build.getTimeInMillis())
                        .withStartTimeInMillis(step.build.getStartTimeInMillis())
                        .withDeliveryID(gogsCause.getDeliveryID()));

            } catch (NotificationException | IOException ex) {
                listener.getLogger().println(Messages.NotificationFailed(ex.getMessage()));

                if (step.failOnError) {
                    throw new AbortException(Messages.NotificationFailed(ex.getMessage()));
                } else {
                    listener.error(Messages.NotificationFailed(ex.getMessage()));
                }
            }

            return null;
        }

        private String firstNonEmpty(String value, String defaultValue) {
            return StringUtils.isNotEmpty(value) ? value : defaultValue;
        }
    }
}
