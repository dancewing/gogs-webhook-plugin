package org.jenkinsci.plugins.gogs.workflow;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gogs.Messages;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Workflow step to send a HipChat room notification.
 */
public class GogsInitStep extends AbstractStepImpl {

    private static final Logger logger = Logger.getLogger(GogsInitStep.class.getName());

    public final RunWrapper build;

    @DataBoundSetter
    public boolean failOnError;

    @DataBoundConstructor
    public GogsInitStep(@Nonnull RunWrapper currentBuild) {
        this.build = currentBuild;
    }

    @Extension
    public static class InitDescriptorImpl extends AbstractStepDescriptorImpl {

        public InitDescriptorImpl() {
            super(GogsInitStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "gogsInit";
        }

        @Override
        public String getDisplayName() {
            return Messages.HipChatSendStepDisplayName();
        }

    }


    public static class GogsInitStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        private transient BuildUtils buildUtils;
        //        @Inject
//        private transient CredentialUtils credentialUtils;
        @Inject
        private transient GogsInitStep step;
        @StepContextParameter
        private transient TaskListener listener;
        @StepContextParameter
        private transient Run<?, ?> run;

        @Override
        protected Void run() throws Exception {

            EnvVars environment = run.getEnvironment(listener);
            if (environment!=null) {
                Set<Map.Entry<String, String>> entries = environment.entrySet();
                Iterator<Map.Entry<String, String>> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> next = iterator.next();
                    logger.info("key :" + next.getKey() +", value: " + next.getValue());
                }
            }

            return null;
        }

        private String firstNonEmpty(String value, String defaultValue) {
            return StringUtils.isNotEmpty(value) ? value : defaultValue;
        }
    }
}
