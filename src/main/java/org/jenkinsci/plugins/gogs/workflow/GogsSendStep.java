package org.jenkinsci.plugins.gogs.workflow;

import hudson.AbortException;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.gogs.CardProvider;
import org.jenkinsci.plugins.gogs.HipChatNotifier;

import org.jenkinsci.plugins.gogs.HipChatService;
import org.jenkinsci.plugins.gogs.impl.NoopCardProvider;
import org.jenkinsci.plugins.gogs.utils.BuildUtils;
import org.jenkinsci.plugins.gogs.utils.CredentialUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gogs.Messages;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Workflow step to send a HipChat room notification.
 */
public class GogsSendStep extends AbstractStepImpl {

    private static final Logger logger = Logger.getLogger(GogsSendStep.class.getName());

    public final String message;

    @DataBoundSetter
    public boolean failOnError;

    @DataBoundConstructor
    public GogsSendStep(@Nonnull String message) {
        this.message = message;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(HipChatSendStepExecution.class);
        }

        public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item context, @QueryParameter String server) {
            return Jenkins.getInstance().getDescriptorByType(HipChatNotifier.DescriptorImpl.class)
                    .doFillCredentialIdItems(context, server);
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

    public static class HipChatSendStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient BuildUtils buildUtils;
        @Inject
        private transient CredentialUtils credentialUtils;
        @Inject
        private transient GogsSendStep step;
        @StepContextParameter
        private transient TaskListener listener;
        @StepContextParameter
        private transient Run<?, ?> run;

        @Override
        protected Void run() throws Exception {
            if (StringUtils.isBlank(step.message)) {
                //allow entire run to fail based on failOnError field
                if (step.failOnError) {
                    throw new AbortException(Messages.MessageRequiredError());
                } else {
                    listener.error(Messages.MessageRequiredError());
                }
                return null;
            }

            //default to global config values if not set in step, but allow step to override all global settings
            HipChatNotifier.DescriptorImpl hipChatDesc =
                    Jenkins.getInstance().getDescriptorByType(HipChatNotifier.DescriptorImpl.class);

//            String room = firstNonEmpty(step.room, hipChatDesc.getRoom());
//            String server = firstNonEmpty(step.server, hipChatDesc.getServer());
//            String sendAs = firstNonEmpty(step.sendAs, hipChatDesc.getSendAs());
//            String credentialId = step.credentialId;
//            String token = null;
//            if (StringUtils.isEmpty(credentialId)) {
//                if (StringUtils.isEmpty(step.token)) {
//                    credentialId = hipChatDesc.getCredentialId();
//                } else {
//                    token = step.token;
//                }
//            }
//
//            if (StringUtils.isNotEmpty(credentialId)) {
//                StringCredentials creds = credentialUtils.resolveCredential(run.getParent(), credentialId, server);
//                if (creds != null) {
//                    token = Secret.toString(creds.getSecret());
//                }
//            }
//            //default to gray if not set in step
//            Color color = step.color != null ? step.color : Color.GRAY;
//            boolean v2enabled = step.v2enabled != null ? step.v2enabled : hipChatDesc.isV2Enabled();

//            HipChatService hipChatService = HipChatNotifier.getHipChatService(server, token, v2enabled, room, sendAs);
//
//            logger.log(Level.FINER, "HipChat publish settings: api v2 - {0} server - {1} token - {2} room - {3}",
//                    new Object[]{v2enabled, server, token, room});

            //attempt to publish message, log NotificationException, will allow run to continue
            try {
                FilePath workspace = null;
                try {
                    workspace = getContext().get(FilePath.class);
                } catch (IOException | InterruptedException ex) {
                    //workspace is not always available, ignore these exceptions
                }
                String message = TokenMacro.expandAll(run, workspace, listener,
                        HipChatNotifier.migrateMessageTemplate(step.message), false, null);

                CardProvider cardProvider = ExtensionList.lookup(CardProvider.class).getDynamic(Jenkins.getInstance()
                        .getDescriptorByType(HipChatNotifier.DescriptorImpl.class)
                        .getCardProvider());
                if (cardProvider == null) {
                    cardProvider = new NoopCardProvider();
                }

//                hipChatService.publish(new Notification()
//                        .withColor(color)
//                        .withMessage(message)
//                        .withCard(cardProvider.getCard(run, listener,
//                                StringUtils.isEmpty(step.icon) ? null : new Icon().withUrl(step.icon), message))
//                        .withNotify(step.notify)
//                        .withMessageFormat(step.textFormat ? MessageFormat.TEXT : MessageFormat.HTML));
               // listener.getLogger().println(Messages.NotificationSuccessful(room));
            } catch (MacroEvaluationException | IOException /*| NotificationException*/ ex) {
                listener.getLogger().println(Messages.NotificationFailed(ex.getMessage()));
                //allow entire run to fail based on failOnError field
                if (step.failOnError) {
                    throw new AbortException(Messages.NotificationFailed(ex.getMessage()));
                } else {
                    listener.error(Messages.NotificationFailed(ex.getMessage()));
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            return null;
        }

        private String firstNonEmpty(String value, String defaultValue) {
            return StringUtils.isNotEmpty(value) ? value : defaultValue;
        }
    }
}
