package hudson.plugins.cigame.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ScoreCardTest {

    @Test(expected=IllegalStateException.class)
    public void testIllegalStateThrownInGetScores() {
        ScoreCard sc = new ScoreCard();
        sc.getScores();
    }

    @Test
    public void assertThatEmptyRuleResultIsNotUsed() {
        Rule rule = mock(Rule.class);
        when(rule.evaluate(isA(AbstractBuild.class))).thenReturn(RuleResult.EMPTY_RESULT);
        ScoreCard card = new ScoreCard();
      AbstractBuild build = mock(AbstractBuild.class);
      when(build.getCulprits()).thenReturn(Collections.emptySet());
        card.record(build, new RuleSet("test", Arrays.asList(new Rule[]{rule})), null);
        assertThat(card.getScores().size(), is(0));
    }
    
    @Test
    public void assertRuleNull(){
    	List<Rule> liste = new ArrayList<Rule>();
    	liste.add(null);
    	ScoreCard card = new ScoreCard();
      AbstractBuild build = mock(AbstractBuild.class);
      when(build.getCulprits()).thenReturn(Collections.emptySet());
    	card.record(build, new RuleSet("test", liste), null);
    }
    
    @Test
    public void assertEmptyRuleBookDoesNotThrowIllegalException() {
        ScoreCard scoreCard = new ScoreCard();
        AbstractBuild build = mock(AbstractBuild.class);
      when(build.getCulprits()).thenReturn(Collections.emptySet());
        scoreCard.record(build, new RuleBook(), null);
        assertThat(scoreCard.getTotalPoints(), is(0d));
    }
}
