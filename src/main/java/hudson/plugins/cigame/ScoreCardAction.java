package hudson.plugins.cigame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.cigame.model.ScoreCard;
import hudson.plugins.cigame.model.ScoreHistoryEntry;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * Score card for a certain build
 *
 * @author Erik Ramfelt
 */
@ExportedBean(defaultVisibility = 999)
public class ScoreCardAction implements Action {

  private static final long serialVersionUID = 1L;

  private AbstractBuild<?, ?> build;

  private ScoreCard scorecard;

  public ScoreCardAction(ScoreCard scorecard, AbstractBuild<?, ?> b) {
    build = b;
    this.scorecard = scorecard;
  }

  public AbstractBuild<?, ?> getBuild() {
    return build;
  }

  public String getDisplayName() {
    return Messages.Scorecard_Title(); //$NON-NLS-1$
  }

  public String getIconFileName() {
    return GameDescriptor.ACTION_LOGO_SMALL;
  }

  public String getUrlName() {
    return "cigame"; //$NON-NLS-1$
  }

  @Exported
  public ScoreCard getScorecard() {
    return scorecard;
  }

  @Exported
  public Collection<User> getParticipants() {
    List<User> players = new ArrayList<User>(scorecard.getAwardedUsers());
    if (players.isEmpty()) {
      scorecard.setUsers(ScoreCard.getUsersFromChanges(build, null));
      players = new ArrayList<User>(scorecard.getAwardedUsers());
    }
    Collections.sort(players, new UserDisplayNameComparator());
    return players;
  }

  private static class UserDisplayNameComparator implements Comparator<User> {

    public int compare(User arg0, User arg1) {
      return arg0.getDisplayName().compareToIgnoreCase(arg1.getDisplayName());
    }
  }
}
