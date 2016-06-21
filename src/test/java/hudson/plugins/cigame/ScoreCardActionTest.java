package hudson.plugins.cigame;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;
import java.util.Iterator;
import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.plugins.cigame.model.ScoreCard;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;

@SuppressWarnings("unchecked")
public class ScoreCardActionTest {

    @Bug(3990)
    @Test 
    public void assertCaseDifferentUserIsReportedAsOneUser() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);        
        ChangeLogSet changeset = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeset);
        
        Iterator<Entry> iterator = Arrays.asList(new Entry[]{mockEntry("Name", "OneName"), mockEntry("name", "TwoName")}).iterator();
        when(changeset.iterator()).thenReturn(iterator);
        ScoreCard scoreCard = mock(ScoreCard.class);
        User user1 = mock(User.class);
      when(user1.getId()).thenReturn("Name");
      when(user1.getDisplayName()).thenReturn("OneName");
      User user2 = mock(User.class);
      when(user2.getId()).thenReturn("name");
      when(user2.getDisplayName()).thenReturn("TwoName");
      when(scoreCard.getAwardedUsers()).thenReturn(Arrays.asList(user1, user2));
        assertThat(new ScoreCardAction(scoreCard, build).getParticipants().size(), is(2));
    }

    @Bug(3990)
    @Test 
    public void assertCaseDifferentUserIsNotReportedAsOneUser() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);        
        ChangeLogSet changeset = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeset);
        Iterator<Entry> iterator = Arrays.asList(new Entry[]{mockEntry("Name", "OneName"), mockEntry("name", "TwoName")}).iterator();
        when(changeset.iterator()).thenReturn(iterator);
      ScoreCard scoreCard = mock(ScoreCard.class);
      User user1 = mock(User.class);
      when(user1.getId()).thenReturn("Name");
      when(user1.getDisplayName()).thenReturn("OneName");
      User user2 = mock(User.class);
      when(user2.getId()).thenReturn("name");
      when(user2.getDisplayName()).thenReturn("TwoName");
      when(scoreCard.getAwardedUsers()).thenReturn(Arrays.asList(user1, user2));
        assertThat(new ScoreCardAction(scoreCard, build).getParticipants().size(), is(2));
    }
    
    @Test 
    public void assertParticipantListIsSorted() {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);        
        ChangeLogSet changeset = mock(ChangeLogSet.class);
        when(build.getChangeSet()).thenReturn(changeset);
        Iterator<Entry> iterator = Arrays.asList(new Entry[]{mockEntry("one", "David"), mockEntry("two", "Barney"), mockEntry("three", "charlie")}).iterator();
        when(changeset.iterator()).thenReturn(iterator);
      ScoreCard scoreCard = mock(ScoreCard.class);
      User user1 = mock(User.class);
      when(user1.getId()).thenReturn("one");
      when(user1.getDisplayName()).thenReturn("David");
      User user2 = mock(User.class);
      when(user2.getId()).thenReturn("two");
      when(user2.getDisplayName()).thenReturn("Barney");
      User user3 = mock(User.class);
      when(user3.getId()).thenReturn("three");
      when(user3.getDisplayName()).thenReturn("charlie");
      when(scoreCard.getAwardedUsers()).thenReturn(Arrays.asList(user1, user2,user3));

        Iterator<User> participantsIterator = new ScoreCardAction(scoreCard, build).getParticipants().iterator();
        assertThat(participantsIterator.next().getDisplayName(), is("Barney"));
        assertThat(participantsIterator.next().getDisplayName(), is("charlie"));
        assertThat(participantsIterator.next().getDisplayName(), is("David"));
    }
    
    private Entry mockEntry(String id, String displayName) {
        User user = mock(User.class);
        when(user.getProperty(UserScoreProperty.class)).thenReturn(new UserScoreProperty(3d, true, null));
        when(user.getDisplayName()).thenReturn(displayName);
        when(user.getId()).thenReturn(id);
        Entry entry = mock(Entry.class);
        when(entry.getAuthor()).thenReturn(user);
        return entry;
    }
}
