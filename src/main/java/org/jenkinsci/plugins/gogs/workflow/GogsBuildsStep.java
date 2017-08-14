package org.jenkinsci.plugins.gogs.workflow;


import com.google.common.base.Splitter;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.gogs.impl.StepStatusUpdater;
import org.jenkinsci.plugins.gogs.model.BuildState;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GogsBuildsStep extends AbstractStepImpl {

    private List<String> builds;

    @DataBoundConstructor
    public GogsBuildsStep() {
    }

    @DataBoundSetter
    public void setBuilds(List<String> builds) {
        if (builds != null && builds.size() == 1) {
            this.builds = new ArrayList<>();
            for (String build : Splitter.on(",").omitEmptyStrings().trimResults().split(builds.get(0))) {
                this.builds.add(build);
            }
        } else {
            this.builds = builds;
        }
    }

    public List<String> getBuilds() {
        return builds;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1;

        @StepContextParameter
        private transient Run<?, ?> run;

        @Inject
        private transient GogsBuildsStep step;

        private BodyExecution body;

        @Override
        public boolean start() throws Exception {
            body = getContext().newBodyInvoker()
                .withCallback(new BodyExecutionCallback() {
                    @Override
                    public void onStart(StepContext context) {
                        for (String name : step.builds) {
                            //CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.pending, name);
                            StepStatusUpdater.updateStepStatus(run, getTaskListener(context), BuildState.pending, name);
                        }
                        run.addAction(new PendingBuildsAction(new ArrayList<>(step.builds)));
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null && !action.getBuilds().isEmpty()) {
                            TaskListener taskListener = getTaskListener(context);
                            if (taskListener != null) {
                                taskListener.getLogger().println("There are still pending GitLab builds. Please check your configuration");
                            }
                        }
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null) {
                            BuildState state = t instanceof FlowInterruptedException ? BuildState.canceled : BuildState.failed;
                            for (String name : action.getBuilds()) {
                               // CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), state, name);
                                StepStatusUpdater.updateStepStatus(run, getTaskListener(context), state, name);
                            }
                        }
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
                PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                if (action != null) {
                    for (String name : action.getBuilds()) {
                     //   CommitStatusUpdater.updateCommitStatus(run, null, BuildState.canceled, name);
                        StepStatusUpdater.updateStepStatus(run, null, BuildState.canceled, name);
                    }
                }
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
            return "Notify gitlab about pending builds";
        }

        @Override
        public String getFunctionName() {
            return "gogsBuilds";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }
}
