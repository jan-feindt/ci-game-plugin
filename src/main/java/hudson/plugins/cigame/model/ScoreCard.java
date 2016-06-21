package hudson.plugins.cigame.model;

import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.cigame.GameDescriptor;
import hudson.plugins.cigame.util.BuildUtil;
import hudson.plugins.cigame.UserScoreProperty;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Score card containing the results of evaluating the rules against a build.
 *
 *
 */
@ExportedBean(defaultVisibility = 999)
public class ScoreCard {

  private List<Score> scores;

  private Map<String, Boolean> users = new HashMap<String, Boolean>();

  private static transient Map<String, User> cachedUsers = new HashMap<String, User>();

  /**
   * Record points for the rules in the rule set
   *
   * @param build build to evaluate
   * @param ruleset rule set to use for evaluation
   * @param listener
   */
  public void record(AbstractBuild<?, ?> build, RuleSet ruleset, BuildListener listener) {

    List<Score> scoresForBuild = new LinkedList<Score>();
    if (ruleset.isActive()) {

      for (Rule rule : ruleset.getRules()) {
        if (null != rule) {
          if (listener != null) {
            listener.getLogger().append("[ci-game] evaluating rule: " + rule.getName() + "\n");
          }
          RuleResult<?> result = evaluate(build, rule);
          if ((result != null) && (result.getPoints() != 0)) {
            Score score = new Score(ruleset.getName(), rule.getName(), result.getPoints(), result.getDescription());
            scoresForBuild.add(score);
            if (listener != null) {
              listener.getLogger().append("[ci-game] scored: " + score.getValue() + "\n");
            }
          }
        } else if (listener != null) {
          listener.getLogger().append("[ci-game] null rule encountered\n");
        }
      }
    } else if (listener != null) {
      listener.getLogger().append("[ci-game] rule: " + ruleset.getName() + "is not active. \n");
    }
    // prevent ConcurrentModificationExceptions for e.g. matrix builds (see JENKINS-11498):
    synchronized (this) {
      if (scores == null) {
        scores = new LinkedList<Score>();
      }
      scores.addAll(scoresForBuild);
      Collections.sort(scores);
      if (!scores.isEmpty()) {
        setUsers(getUsersFromChanges(build, listener));
      }
    }
  }

  public static Map<String, Boolean> getUsersFromChanges(AbstractBuild<?, ?> build, BuildListener listener) {
    Map<String, Boolean> l_userIds = new HashMap<String, Boolean>();
    ChangeLogSet<? extends Entry> changeSet = build.getChangeSet();
    for (Entry entry : changeSet) {
      User user = entry.getAuthor();
      if (listener != null) {
        listener.getLogger().println("Prüfe User " + user.getId());
      }
      UserScoreProperty property = user.getProperty(UserScoreProperty.class);
      String l_entryUserId = user.getId();
      if (((property == null)
              || property.isParticipatingInGame()) && !l_userIds.containsKey(l_entryUserId)) {
        l_userIds.put(l_entryUserId, true);
        if (listener != null) {
          listener.getLogger().println("User " + user.getId() + " ist beteiligt.");
        }

      }
    }
    return l_userIds;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  RuleResult<?> evaluate(AbstractBuild<?, ?> build, Rule rule) {
    if (rule instanceof AggregatableRule<?> && build instanceof MavenModuleSetBuild) {
      AggregatableRule aRule = (AggregatableRule<?>) rule;
      MavenModuleSetBuild mavenModuleSetBuild = (MavenModuleSetBuild) build;

      List<RuleResult> results = new ArrayList<RuleResult>();

      for (Map.Entry<MavenModule, MavenBuild> e : mavenModuleSetBuild.getModuleLastBuilds().entrySet()) {
        MavenBuild moduleBuild = e.getValue();
        if (moduleBuild != null) {
          AbstractBuild<?, ?> previousBuild = BuildUtil.getPreviousBuiltBuild(moduleBuild);
          results.add(aRule.evaluate(previousBuild, moduleBuild));
        } else // module was probably removed from multimodule
        {
          if (mavenModuleSetBuild.getPreviousBuild() != null) {
            MavenModuleSetBuild prevBuild = mavenModuleSetBuild.getPreviousBuild();
            AbstractBuild<?, ?> prevModuleBuild = prevBuild.getModuleLastBuilds().get(e.getKey());
            if (prevModuleBuild.getResult() == null) {
              prevModuleBuild = BuildUtil.getPreviousBuiltBuild(prevModuleBuild);
            }
            results.add(aRule.evaluate(prevModuleBuild, null));
          } else {
            //results.add(aRule.evaluate(null, null));
            return RuleResult.EMPTY_RESULT;
          }
        }
      }
      return aRule.aggregate(results);
    } else if (rule instanceof AggregatableRule<?>) {
      AggregatableRule<?> aRule = (AggregatableRule<?>) rule;
      return aRule.evaluate(build.getPreviousBuild(), build);
    } else {
      return rule.evaluate(build);
    }
  }

  /**
   * Record points for the rules in the rule book
   *
   * @param build build to evaluate
   * @param ruleBook rule book to use for evaluation
   * @param listener
   */
  public void record(AbstractBuild<?, ?> build, RuleBook ruleBook, BuildListener listener) {
    if (scores == null) {
      scores = new LinkedList<Score>();
    }
    for (RuleSet set : ruleBook.getRuleSets()) {
      record(build, set, listener);
    }
  }

  /**
   * Returns a collection of scores. May not be called before the score has been
   * recorded.
   *
   * @return a collection of scores.
   * @throws IllegalStateException thrown if the method is called before the
   * scores has been recorded.
   */
  @Exported
  public Collection<Score> getScores() throws IllegalStateException {
    if (scores == null) {
      throw new IllegalStateException("No scores are available"); //$NON-NLS-1$
    }
    return scores;
  }

  /**
   * Returns the total points for this score card
   *
   * @return the total points for this score card
   * @throws IllegalStateException thrown if the method is called before scores
   * has been calculated
   */
  @Exported
  public double getTotalPoints() throws IllegalStateException {
    if (scores == null) {
      throw new IllegalStateException("No scores are available"); //$NON-NLS-1$
    }
    double value = 0;
    for (Score score : scores) {
      value += score.getValue();
    }
    return value;
  }

  @Exported
  public List<User> getAwardedUsers() {
    if (users == null) {
      return Collections.emptyList();
    } else {
      List<User> l_return = new ArrayList<User>();
      for (java.util.HashMap.Entry<String, Boolean> userId : users.entrySet()) {
        if (userId.getValue()) {
          if (cachedUsers.containsKey(userId.getKey())) {
            l_return.add(cachedUsers.get(userId.getKey()));
          } else {
            User l_user = User.get(userId.getKey());
            if (l_user != null) {
              l_return.add(l_user);
              cachedUsers.put(userId.getKey(), l_user);
            }
          }
        }
      }
      return l_return;
    }
  }

  /**
   * @param users the users to set
   */
  public void setUsers(Map<String, Boolean> users) {
    this.users = users;
  }
  
  public static void addToCache(String p_id, User p_user) {
    cachedUsers.put(p_id, p_user);
  }
  
  public static void resetCache() {
    cachedUsers.clear();
  }
}
