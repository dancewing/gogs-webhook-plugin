package org.jenkinsci.plugins.gogs.ext.tokens;

import static org.jenkinsci.plugins.gogs.model.Constants.*;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class BuildDescriptionMacro extends TokenMacro {

    @Override
    public boolean acceptsMacroName(String macroName) {
        return BUILD_DESCRIPTION.equals(macroName);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName,
            Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) {
        return evaluate(context, null, listener, macroName, arguments, argumentMultimap);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener,
            String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) {
        return run.getDescription();
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(BUILD_DESCRIPTION);
    }
}
