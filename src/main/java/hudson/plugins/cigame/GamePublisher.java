package hudson.plugins.cigame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.model.Cause;
import hudson.model.TopLevelItem;
import hudson.model.Run;
import hudson.model.Hudson;
import hudson.plugins.cigame.model.RuleBook;
import hudson.plugins.cigame.model.ScoreCard;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import org.kohsuke.stapler.DataBoundConstructor;

public class GamePublisher extends Notifier {

  private boolean activateUnittestPoints = true;
  private boolean activateBuildPoints = true;

  @DataBoundConstructor
  public GamePublisher(Boolean activateBuildPoints, Boolean activateUnittestPoints) {
    this.activateUnittestPoints = activateUnittestPoints;
    this.activateBuildPoints = activateBuildPoints;
  }

  @Override
  public GameDescriptor getDescriptor() {
    return (GameDescriptor) super.getDescriptor();
  }

  @Override
  public boolean needsToRunAfterFinalized() {
    return true;
  }

  @Override
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return null;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
          BuildListener listener) throws InterruptedException, IOException {

    listener.getLogger().println("Rule Build Points is " + (activateBuildPoints ? "activ" : "not activ"));
    listener.getLogger().println("Unittest Points is " + (activateUnittestPoints ? "activ" : "not activ"));

    perform(build, getDescriptor().getRuleBook(activateBuildPoints, activateUnittestPoints), getDescriptor().getNamesAreCaseSensitive(), listener);

    return true;
  }

  /**
   * Calculates score from the build and rule book and adds a Game action to the
   * build.
   *
   * @param build build to calculate points for
   * @param ruleBook rules used in calculation
   * @param usernameIsCasesensitive user names in Hudson are case insensitive.
   * @param listener the build listener
   * @return true, if any user scores were updated; false, otherwise
   * @throws IOException thrown if there was a problem setting a user property
   */
  boolean perform(AbstractBuild<?, ?> build, RuleBook ruleBook, boolean usernameIsCasesensitive, BuildListener listener) throws IOException {
    ScoreCard sc = new ScoreCard();
    sc.record(build, ruleBook, listener);

    ScoreCardAction action = new ScoreCardAction(sc, build);
    build.getActions().add(action);

    List<AbstractBuild<?, ?>> accountableBuilds = new ArrayList<AbstractBuild<?, ?>>();
    accountableBuilds.add(build);

    AbstractBuild upstreamBuild = getBuildByUpstreamCause(build.getCauses(), listener);
    if (upstreamBuild != null) {
      accountableBuilds.add(upstreamBuild);
      ChangeLogSet<? extends Entry> changeSet = upstreamBuild.getChangeSet();
      if (listener != null) {
        listener.getLogger().append("[ci-game] UpStream Build ID: " + upstreamBuild.getId() + "\n");
      }
      if (listener != null) {
        listener.getLogger().append("[ci-game] UpStream Display Name: " + upstreamBuild.getFullDisplayName() + "\n");
      }
      if (listener != null) {
        listener.getLogger().append("[ci-game] Is UpStream Change Set Empty: " + changeSet.isEmptySet() + "\n");
      }

    }

    // also add all previous aborted builds:
    AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
    while (previousBuild != null && previousBuild.getResult() == Result.ABORTED) {
      accountableBuilds.add(previousBuild);
      previousBuild = previousBuild.getPreviousBuild();
    }

    Set<User> players = new TreeSet<User>(usernameIsCasesensitive ? null : new UsernameCaseinsensitiveComparator());
    for (AbstractBuild<?, ?> b : accountableBuilds) {
      ChangeLogSet<? extends Entry> changeSet = b.getChangeSet();
      if (changeSet != null) {
        for (Entry e : changeSet) {
          players.add(e.getAuthor());
        }
      }
    }

    return updateUserScores(players, sc.getTotalPoints(), accountableBuilds);
  }

  private AbstractBuild getBuildByUpstreamCause(List<Cause> causes, BuildListener listener) {
    for (Cause cause : (List<Cause>) causes) {
      if (cause instanceof Cause.UpstreamCause) {
        TopLevelItem upstreamProject = Hudson.getInstance().getItemByFullName(((Cause.UpstreamCause) cause).getUpstreamProject(), TopLevelItem.class);
        if (upstreamProject instanceof AbstractProject) {
          int buildId = ((Cause.UpstreamCause) cause).getUpstreamBuild();
          Run run = ((AbstractProject) upstreamProject).getBuildByNumber(buildId);
          System.out.println();
          AbstractBuild upstreamRun = getBuildByUpstreamCause(run.getCauses(), listener);
          if (upstreamRun == null) {
            return (AbstractBuild) run;
          } else {
            return upstreamRun;
          }
        }
      }
    }
    return null;

  }

  /**
   * Add the score to the users that have committed code in the change set
   *
   *
   * @param score the score that the build was worth
   * @param accountableBuilds the builds for which the {@code score} is awarded
   * for.
   * @throws IOException thrown if the property could not be added to the user
   * object.
   * @return true, if any user scores was updated; false, otherwise
   */
  private boolean updateUserScores(Set<User> players, double score, List<AbstractBuild<?, ?>> accountableBuilds) throws IOException {
    if (score != 0) {
      for (User user : players) {
        UserScoreProperty property = user.getProperty(UserScoreProperty.class);
        if (property == null) {
          property = new UserScoreProperty();
          user.addProperty(property);
        }
        if (property.isParticipatingInGame()) {
          property.setScore(property.getScore() + score);
          property.rememberAccountableBuilds(accountableBuilds, score);
        }
        user.save();
      }
    }
    return (!players.isEmpty());
  }

  /**
   * @return the activateUnittestPoints
   */
  public boolean isActivateUnittestPoints() {
    return activateUnittestPoints;
  }

  /**
   * @param activateUnittestPoints the activateUnittestPoints to set
   */
  public void setActivateUnittestPoints(boolean activateUnittestPoints) {
    this.activateUnittestPoints = activateUnittestPoints;
  }

  /**
   * @return the activateBuildPoints
   */
  public boolean isActivateBuildPoints() {
    return activateBuildPoints;
  }

  /**
   * @param activateBuildPoints the activateBuildPoints to set
   */
  public void setActivateBuildPoints(boolean activateBuildPoints) {
    this.activateBuildPoints = activateBuildPoints;
  }

  public static class UsernameCaseinsensitiveComparator implements Comparator<User> {

    public int compare(User arg0, User arg1) {
      return arg0.getId().compareToIgnoreCase(arg1.getId());
    }
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }
}
