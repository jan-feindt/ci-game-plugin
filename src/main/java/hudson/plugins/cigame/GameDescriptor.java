package hudson.plugins.cigame;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.cigame.model.RuleBook;
import hudson.plugins.cigame.model.RuleSet;
import hudson.plugins.cigame.model.ScoreLevel;
import hudson.plugins.cigame.rules.build.BuildRuleSet;
import hudson.plugins.cigame.rules.plugins.checkstyle.CheckstyleRuleSet;
import hudson.plugins.cigame.rules.plugins.findbugs.FindBugsRuleSet;
import hudson.plugins.cigame.rules.plugins.opentasks.OpenTasksRuleSet;
import hudson.plugins.cigame.rules.plugins.pmd.PmdRuleSet;
import hudson.plugins.cigame.rules.plugins.violation.ViolationsRuleSet;
import hudson.plugins.cigame.rules.plugins.warnings.WarningsRuleSet;
import hudson.plugins.cigame.rules.unittesting.UnitTestingRuleSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

// Config page for the application (descriptor of the game plugin)
@Extension
public class GameDescriptor extends BuildStepDescriptor<Publisher> {

  public static final String ACTION_LOGO_LARGE = "/plugin/ci-game/icons/game-32x32.png"; //$NON-NLS-1$
  public static final String ACTION_LOGO_MEDIUM = "/plugin/ci-game/icons/game-22x22.png"; //$NON-NLS-1$
  private transient List<ScoreLevel> scoreLevels;
  private boolean namesAreCaseSensitive = true;
  private int passedTestIncreasingPoints = 1;
  private int passedTestDecreasingPoints = 0;
  private int failedTestIncreasingPoints = -1;
  private int failedTestDecreasingPoints = 0;
  private int skippedTestIncreasingPoints = 0;
  private int skippedTestDecreasingPoints = 0;

  private int successfulBuildPoints = 1;
  private int failedBuildPoints = -10;

  public GameDescriptor() {
    super(GamePublisher.class);
    load();
  }

  /**
   * Returns the default rule book
   *
   * @return the rule book that is configured for the game.
   */
  public RuleBook getRuleBook(boolean p_activateBuildPoints, boolean p_activateUnittestPoints) {

    // add default-rules 
    RuleBook rulebook = new RuleBook();

    BuildRuleSet buildRuleSet = new BuildRuleSet(getSuccessfulBuildPoints(), getFailedBuildPoints());
    // add Job-specific rules
    if (!p_activateBuildPoints) {
      buildRuleSet.deactivate();
    }
    addRuleSetIfAvailable(rulebook, buildRuleSet);

    UnitTestingRuleSet unitTestingRuleSet = new UnitTestingRuleSet();
    if (!p_activateUnittestPoints) {
      unitTestingRuleSet.deactivate();
    }
    addRuleSetIfAvailable(rulebook, unitTestingRuleSet);

    addRuleSetIfAvailable(rulebook, new OpenTasksRuleSet());
    addRuleSetIfAvailable(rulebook, new ViolationsRuleSet());
    addRuleSetIfAvailable(rulebook, new PmdRuleSet());
    addRuleSetIfAvailable(rulebook, new FindBugsRuleSet());
    addRuleSetIfAvailable(rulebook, new WarningsRuleSet());
    addRuleSetIfAvailable(rulebook, new CheckstyleRuleSet());

    return rulebook;
  }

  private void addRuleSetIfAvailable(RuleBook book, RuleSet ruleSet) {
    if (ruleSet.isAvailable()) {
      book.addRuleSet(ruleSet);
    }
  }

  // config page heading
  @Override
  public String getDisplayName() {
    return Messages.Plugin_Title();
  }

  public List<ScoreLevel> getScoreLevels() {
    if (scoreLevels == null) {
      String jsonScoreLevels = "[]";
      try {
        InputStream inputStream = this.getClass().getResourceAsStream("/score-levels.json");
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, "UTF-8");
        jsonScoreLevels = writer.toString();
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      Gson gson = new Gson();
      Type listType = new TypeToken<List<ScoreLevel>>() {
      }.getType();
      scoreLevels = gson.fromJson(jsonScoreLevels, listType);
      String rootUrl = Hudson.getInstance().getRootUrl();
      for (ScoreLevel scoreLevel : scoreLevels) {
        String imageUrl = scoreLevel.getImageUrl();
        if (imageUrl.startsWith("/")) {
          scoreLevel.setImageUrl(rootUrl + imageUrl);
        }
      }
    }
    return scoreLevels;
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
    req.bindJSON(this, json);
    save();
    return super.configure(req, json);
  }

  public boolean getNamesAreCaseSensitive() {
    return namesAreCaseSensitive;
  }

  public void setNamesAreCaseSensitive(boolean namesAreCaseSensitive) {
    this.namesAreCaseSensitive = namesAreCaseSensitive;
  }

  @Override
  public boolean isApplicable(Class<? extends AbstractProject> arg0) {
    return true;
  }

  public int getPassedTestIncreasingPoints() {
    return passedTestIncreasingPoints;
  }

  public void setPassedTestIncreasingPoints(int passedTestIncreasingPoints) {
    this.passedTestIncreasingPoints = passedTestIncreasingPoints;
  }

  public int getPassedTestDecreasingPoints() {
    return passedTestDecreasingPoints;
  }

  public void setPassedTestDecreasingPoints(int passedTestDecreasingPoints) {
    this.passedTestDecreasingPoints = passedTestDecreasingPoints;
  }

  public int getFailedTestIncreasingPoints() {
    return failedTestIncreasingPoints;
  }

  public void setFailedTestIncreasingPoints(int failedTestIncreasingPoints) {
    this.failedTestIncreasingPoints = failedTestIncreasingPoints;
  }

  public int getFailedTestDecreasingPoints() {
    return failedTestDecreasingPoints;
  }

  public void setFailedTestDecreasingPoints(int failedTestDecreasingPoints) {
    this.failedTestDecreasingPoints = failedTestDecreasingPoints;
  }

  public int getSkippedTestIncreasingPoints() {
    return skippedTestIncreasingPoints;
  }

  public void setSkippedTestIncreasingPoints(int skippedTestIncreasingPoints) {
    this.skippedTestIncreasingPoints = skippedTestIncreasingPoints;
  }

  public int getSkippedTestDecreasingPoints() {
    return skippedTestDecreasingPoints;
  }

  public void setSkippedTestDecreasingPoints(int skippedTestDecreasingPoints) {
    this.skippedTestDecreasingPoints = skippedTestDecreasingPoints;
  }

  /**
   * @return the successfulBuildPoints
   */
  public int getSuccessfulBuildPoints() {
    return successfulBuildPoints;
  }

  /**
   * @param successfulBuildPoints the successfulBuildPoints to set
   */
  public void setSuccessfulBuildPoints(int successfulBuildPoints) {
    this.successfulBuildPoints = successfulBuildPoints;
  }

  /**
   * @return the failedBuildPoints
   */
  public int getFailedBuildPoints() {
    return failedBuildPoints;
  }

  /**
   * @param failedBuildPoints the failedBuildPoints to set
   */
  public void setFailedBuildPoints(int failedBuildPoints) {
    this.failedBuildPoints = failedBuildPoints;
  }

  public String exportScores() {
    LeaderBoardAction page = new LeaderBoardAction();
    Gson gson = new Gson();
    return gson.toJson(page.getUserScores());
  }

  public void importScores(String json) {
    LeaderBoardAction page = new LeaderBoardAction();
    Gson gson = new Gson();
    Type collectionType = new TypeToken<List<LeaderBoardAction.UserScore>>() {
    }.getType();
    Collection<User> users = User.getAll();

    List<LeaderBoardAction.UserScore> list = gson.fromJson("", collectionType);
    for (LeaderBoardAction.UserScore userScore : list) {
      UserScoreProperty property = new UserScoreProperty(userScore.getScore(), true, null);
      for (User user : users) {
        if (user.getId().equals(property.getUser().getId())) {
          if (user.getId().equals(property.getUser().getId())) {
            try {
              user.addProperty(property);
            } catch (IOException e) {
              e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
          }
        }
      }
    }
  }
}
