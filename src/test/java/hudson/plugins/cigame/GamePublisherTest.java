package hudson.plugins.cigame;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.User;
import hudson.plugins.cigame.model.Rule;
import hudson.plugins.cigame.model.RuleBook;
import hudson.plugins.cigame.model.RuleResult;
import hudson.plugins.cigame.model.RuleSet;
import hudson.plugins.cigame.model.ScoreCard;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.lang.reflect.Field;
import jenkins.model.IdStrategy;
import org.junit.BeforeClass;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
public class GamePublisherTest {

  @Test
  public void assertScoreCardActionIsAddedToBuild() throws Exception {
    AbstractBuild build = mock(AbstractBuild.class);
    List<Action> actions = mock(List.class);
    when(build.getActions()).thenReturn(actions);

    assertThat(new GamePublisher(true, true).perform(build, new RuleBook(), null), is(false));

    verify(build).getActions();
    verify(actions).add(Mockito.isA(ScoreCardAction.class));
    
    verify(build).getPreviousBuild();
    verify(build).getCauses();
    verifyNoMoreInteractions(build);
  }

  @Test
  public void assertPointsAreToExistingUserScoreProperty() throws Exception {
    AbstractBuild build = mock(AbstractBuild.class);
    UserScoreProperty userScoreProperty = new UserScoreProperty(10, true, null);
    User createUser = createUser(userScoreProperty);
    mockChangeSetInBuild(build, createUser);
    ScoreCard.resetCache();
    ScoreCard.addToCache(createUser.getId(), createUser);

    assertThat(new GamePublisher(true, true).perform(build, createRuleBook(5d),  null), is(true));
    assertThat(userScoreProperty.getScore(), is(15d));
  }

  @Test
  public void assertUserScorePropertyIsAddedToUserThatDoesNotHaveIt() throws Exception {
    AbstractBuild build = mock(AbstractBuild.class);
    User userWithoutProperty = createUser(null);
    mockChangeSetInBuild(build, userWithoutProperty);
    ScoreCard.resetCache();
    ScoreCard.addToCache(userWithoutProperty.getId(), userWithoutProperty);

    assertThat(new GamePublisher(true, true).perform(build, createRuleBook(5d),  null), is(true));
    verify(userWithoutProperty).addProperty(new UserScoreProperty(5, true, anyList()));
  }

  @Bug(4470)
  @Test
  public void assertThatUserDoesNotReciveDoublePointsIfUserExistInSeveralChangeSetEntries() throws Exception {
    AbstractBuild build = mock(AbstractBuild.class);
    UserScoreProperty property = new UserScoreProperty(10d, true, null);
    User user = createUser(property);
    mockChangeSetInBuild(build, user, user);
    ScoreCard.resetCache();
    ScoreCard.addToCache(user.getId(), user);

    assertThat(new GamePublisher(true, true).perform(build, createRuleBook(5d),  null), is(true));
    assertThat(property.getScore(), is(15d));
  }

  @Test
  public void assertUsersNamesWithDifferentCasingIsReportedAsPointsForOneUser() throws Exception {
    AbstractBuild build = mock(AbstractBuild.class);
    UserScoreProperty propertyOne = new UserScoreProperty(10, true, null);
    UserScoreProperty propertyTwo = new UserScoreProperty(20, true, null);
    User createUser1 = createUser(propertyOne, "name");
    User createUser2 = createUser(propertyTwo, "NAME");
    
    mockChangeSetInBuild(build, createUser1, createUser2);

    ScoreCard.resetCache();
    ScoreCard.addToCache(createUser1.getId(), createUser1);
    ScoreCard.addToCache(createUser2.getId(), createUser2);
    
    assertThat(new GamePublisher(true, true).perform(build, createRuleBook(5d),  null), is(true));
    assertThat(propertyOne.getScore(), is(15d));
    assertThat("Points were added to both users", propertyTwo.getScore(), is(25d));
  }

  @Test
  public void assertUsersAreLookedUpCaseInsensitive() throws Exception {
    AbstractBuild build = mock(AbstractBuild.class);
    UserScoreProperty propertyOne = new UserScoreProperty(10, true, null);
    User existingUser = createUser(propertyOne, "name");
    User createUser = createUser(null, "NAME");
    mockChangeSetInBuild(build, createUser);
    
    ScoreCard.resetCache();
    ScoreCard.addToCache(existingUser.getId(), existingUser);
    ScoreCard.addToCache(createUser.getId(), createUser);

    assertThat(new GamePublisher(true, true).perform(build, createRuleBook(15d),  null), is(true));
    assertThat(propertyOne.getScore(), is(10d));
  }

  private RuleBook createRuleBook(double points) {
    return new RuleBook(new RuleSet("test", new ArrayList<Rule>(Arrays.asList(new RuleImpl(new RuleResult(points, "desc"))))));
  }

  private void mockChangeSetInBuild(AbstractBuild build, User... users) {
    ChangeLogSet changeset = mock(ChangeLogSet.class);
    when(build.getActions()).thenReturn(new ArrayList<Action>());
    List<Entry> changesetList = new ArrayList();
    for (User user : users) {
      changesetList.add(createEntry(user));
    }
    when(changeset.iterator()).thenReturn(changesetList.iterator());
    when(build.getChangeSet()).thenReturn(changeset);
  }

  private User createUser(UserScoreProperty property) {
    return createUser(property, "ignored-" + System.currentTimeMillis());
  }

  private User createUser(UserScoreProperty property, String name) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(name);
    if (property != null) {
      when(user.getProperty(UserScoreProperty.class)).thenReturn(property);
    }
    return user;
  }

  private Entry createEntry(User author) {
    Entry entry = mock(Entry.class);
    when(entry.getAuthor()).thenReturn(author);
    return entry;
  }

  private static class RuleImpl implements Rule {

    private final RuleResult ruleResult;

    public RuleImpl(RuleResult ruleResult) {
      this.ruleResult = ruleResult;
    }

    public RuleResult evaluate(AbstractBuild<?, ?> build) {
      return ruleResult;
    }

    public String getName() {
      return "impl";
    }
  }
}
