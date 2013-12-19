package hudson.plugins.cigame;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hudson.Extension;
import hudson.model.Api;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.model.User;

import hudson.plugins.cigame.model.ScoreLevel;

import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.VersionNumber;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Leader board for users participaing in the game.
 *
 * @author Erik Ramfelt
 */
@ExportedBean(defaultVisibility = 999)
@Extension
public class LeaderBoardAction implements RootAction, AccessControlled {

  private static final long serialVersionUID = 1L;

  public String getDisplayName() {
    return Messages.Leaderboard_Title();
  }

  public String getIconFileName() {
    return GameDescriptor.ACTION_LOGO_MEDIUM;
  }

  public String getUrlName() {
    return "/cigame"; //$NON-NLS-1$
  }

  /**
   * Returns the user that are participants in the ci game
   *
   * @return list containing users.
   */
  @Exported
  public List<UserScore> getUserScores() {
    return getUserScores(User.getAll(), Hudson.getInstance().getDescriptorByType(GameDescriptor.class).getNamesAreCaseSensitive());
  }

  @Exported
  public boolean isUserAvatarSupported() {
    return new VersionNumber(Hudson.VERSION).isNewerThan(new VersionNumber("1.433"));
  }

  public List<UserScore> getUserScores(Collection<User> users, boolean usernameIsCaseSensitive) {
    List<UserScore> list = new ArrayList<UserScore>();

    Collection<User> players;
    if (usernameIsCaseSensitive) {
      players = users;
    } else {
      List<User> playerList = new ArrayList<User>();
      CaseInsensitiveUserIdComparator caseInsensitiveUserIdComparator = new CaseInsensitiveUserIdComparator();
      for (User user : users) {
        if (Collections.binarySearch(playerList, user, caseInsensitiveUserIdComparator) < 0) {
          playerList.add(user);
        }
      }
      players = playerList;
    }

    for (User user : players) {
      UserScoreProperty property = user.getProperty(UserScoreProperty.class);
      if ((property != null) && property.isParticipatingInGame()) {
        list.add(new UserScore(user, property.getScore(), user.getDescription()));
      }
    }

    Collections.sort(list, new Comparator<UserScore>() {
      public int compare(UserScore o1, UserScore o2) {
        if (o1.score < o2.score) {
          return 1;
        }
        if (o1.score > o2.score) {
          return -1;
        }
        return 0;
      }
    });

    return list;
  }

  public void doResetScores(StaplerRequest req, StaplerResponse rsp) throws IOException {
    User current = User.current();
    if (current != null && current.hasPermission(Hudson.ADMINISTER)) {
      resetScores(User.getAll());
    }
    rsp.sendRedirect2(req.getContextPath());
  }

  public void resetScores(Collection<User> users) throws IOException {
    for (User user : users) {
      UserScoreProperty property = user.getProperty(UserScoreProperty.class);
      if (property != null) {
        property.setScore(0);
        property.setScoreHistoryEntries(null);
        user.save();
      }
    }
  }

  public void doExportScores(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    rsp.setContentType("application/json");
    rsp.setHeader("Content-Disposition", "inline; filename=scores.json");

    String jsonString = exportScores();
    InputStream in = new ByteArrayInputStream(jsonString.getBytes());
    rsp.serveFile(req, in, 0, -1, jsonString.length(), "scores.json");
  }

  public String exportScores() {
    Gson gson = new Gson();
    Collection<User> users = User.getAll();
    List<UserScoreProperty> scores = new ArrayList<UserScoreProperty>();
    for (User user : users) {
      UserScoreProperty property = user.getProperty(UserScoreProperty.class);
      if ((property != null) && property.isParticipatingInGame()) {
        property.setUserId(user.getId());
        scores.add(property);
      }
    }
    return gson.toJson(scores);
  }

  public void doImportScores(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    User current = User.current();
    if (current != null && current.hasPermission(Hudson.ADMINISTER)) {
      JSONObject form = req.getSubmittedForm();
      FileItem fileItem = req.getFileItem(form.getString("jsonData"));
      InputStream inputStream = fileItem.getInputStream();
      StringWriter writer = new StringWriter();
      IOUtils.copy(inputStream, writer, "UTF-8");
      importScores(writer.toString());
    }
    rsp.sendRedirect2(req.getContextPath());
  }

  public void importScores(String json) throws IOException {
    Gson gson = new Gson();
    Type collectionType = new TypeToken<List<UserScoreProperty>>() {
    }.getType();
    List<UserScoreProperty> list = gson.fromJson(json, collectionType);

    Collection<User> users = User.getAll();

    for (UserScoreProperty scoreProperty : list) {
      for (User user : users) {
        if (user.getId().equals(scoreProperty.getUserId())) {
          UserScoreProperty property = user.getProperty(UserScoreProperty.class);
          if (property == null) {
            user.addProperty(scoreProperty);
          } else {
            property.setScore(scoreProperty.getScore());
            property.setNotParticipatingInGame(!scoreProperty.isParticipatingInGame());
            property.setScoreHistoryEntries(scoreProperty.getMostRecentScores());
          }
          user.save();
        }
      }
    }
  }

  public ACL getACL() {
    return Hudson.getInstance().getACL();
  }

  public void checkPermission(Permission p) {
    getACL().checkPermission(p);
  }

  public boolean hasPermission(Permission p) {
    return getACL().hasPermission(p);
  }

  @Exported
  public Map<ScoreLevel, List<UserScore>> getUserGroups() {
    Map<ScoreLevel, List<UserScore>> result = new TreeMap<ScoreLevel, List<UserScore>>(new DescendingScoreLevelComparator());

    GameDescriptor gameDescriptor = Hudson.getInstance().getDescriptorByType(GameDescriptor.class);
    List<UserScore> userScores = getUserScores(User.getAll(), gameDescriptor.getNamesAreCaseSensitive());
    List<ScoreLevel> levels = gameDescriptor.getScoreLevels();

    for (UserScore userScore : userScores) {
      ScoreLevel chosenLevel = null;
      for (ScoreLevel level : levels) {
        if (userScore.getScore() >= level.getMinScore() && userScore.getScore() < level.getMaxScore()) {
          chosenLevel = level;
          break;
        }
      }

      addUserScoreToLevel(userScore, chosenLevel, result);
    }

    return result;
  }

  private void addUserScoreToLevel(UserScore userScore, ScoreLevel level, Map<ScoreLevel, List<UserScore>> userScores) {
    List<UserScore> scores = userScores.get(level);
    if (scores == null) {
      scores = new ArrayList<UserScore>();
      userScores.put(level, scores);
    }
    scores.add(userScore);
  }

  private class DescendingScoreLevelComparator implements Comparator<ScoreLevel> {

        Collection<User> users = User.getAll();

        for (UserScoreProperty scoreProperty : list) {
            for (User user : users) {
                if (user.getId().equals(scoreProperty.getUserId())) {
                    UserScoreProperty property = user.getProperty(UserScoreProperty.class);
                    if (property == null) {
                        user.addProperty(scoreProperty);
                    } else {
                        property.setScore(scoreProperty.getScore());
                        property.setNotParticipatingInGame(!scoreProperty.isParticipatingInGame());
                    }
                    user.save();
                }
            }
        }
    }
  }

  @ExportedBean(defaultVisibility = 999)
  public class UserScore {

    private User user;
    private double score;
    private String description;

    public UserScore(User user, double score, String description) {
      super();
      this.user = user;
      this.score = score;
      this.description = description;
    }

    @Exported
    public User getUser() {
      return user;
    }

    @Exported
    public double getScore() {
      return score;
    }

    @Exported
    public String getDescription() {
      return description;
    }
  }

  public Api getApi() {
    return new Api(this);
  }
}
