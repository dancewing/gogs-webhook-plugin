package org.jenkinsci.plugins.gogs.workflow;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.gogs.impl.StepStatusUpdater;
import org.jenkinsci.plugins.gogs.model.BuildState;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GogsStatusReportStep extends AbstractStepImpl {

    private String name;

    @DataBoundConstructor
    public GogsStatusReportStep(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1;

        @StepContextParameter
        private transient Run<?, ?> run;

        @Inject
        private transient GogsStatusReportStep step;

        private BodyExecution body;

        @Override
        public boolean start() throws Exception {
            final String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
            body = getContext().newBodyInvoker()
                .withCallback(new BodyExecutionCallback() {
                    @Override
                    public void onStart(StepContext context) {
                        StepStatusUpdater.updateStepStatus(run, getTaskListener(context), BuildState.running, name);
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null) {
                            action.startBuild(name);
                        }
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        StepStatusUpdater.updateStepStatus(run, getTaskListener(context), BuildState.success, name);
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        BuildState state = t instanceof FlowInterruptedException ? BuildState.canceled : BuildState.failed;
                        StepStatusUpdater.updateStepStatus(run, getTaskListener(context), state, name);
                        context.onFailure(t);
                    }
                })
                .start();
            return false;
        }

        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            // should be no need to do anything special (but verify in JENKINS-26148)
            if (body != null) {
                String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
                StepStatusUpdater.updateStepStatus(run, null, BuildState.canceled, name);
                body.cancel(cause);
            }
        }

        private TaskListener getTaskListener(StepContext context) {
            if (!context.isReady()) {
                return null;
            }
            try {
                return context.get(TaskListener.class);
            } catch (Exception x) {
                return null;
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getDisplayName() {
            return "Update the commit status in GitLab depending on the build status";
        }

        @Override
        public String getFunctionName() {
            return "gogsReportStatus";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }
}
