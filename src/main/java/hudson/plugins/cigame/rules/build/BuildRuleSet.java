package hudson.plugins.cigame.rules.build;

import hudson.plugins.cigame.model.RuleSet;

/**
 * Rule set for common build rules.
 */
public class BuildRuleSet extends RuleSet {
    public BuildRuleSet(int p_successfulBuildPoints, int p_failedBuildPoints) {
        super(Messages.BuildRuleSet_Title()); //$NON-NLS-1$
        add(new BuildResultRule(p_successfulBuildPoints, p_failedBuildPoints));
    }
}
