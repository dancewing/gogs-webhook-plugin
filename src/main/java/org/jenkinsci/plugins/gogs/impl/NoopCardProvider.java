package org.jenkinsci.plugins.gogs.impl;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.gogs.CardProvider;
import org.jenkinsci.plugins.gogs.CardProviderDescriptor;
import jenkins.plugins.hipchat.model.notifications.Card;
import jenkins.plugins.hipchat.model.notifications.Icon;
import org.jenkinsci.plugins.gogs.Messages;

@Extension
public class NoopCardProvider extends CardProvider {

    @Override
    public Card getCard(Run<?, ?> run, TaskListener taskListener, Icon icon, String message) {
        return null;
    }

    @Override
    public CardProviderDescriptor getDescriptor() {
        return new DescriptorImpl();
    }
    
    @Extension
    public static class DescriptorImpl extends CardProviderDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.NoopCardProvider();
        }
    }
}
