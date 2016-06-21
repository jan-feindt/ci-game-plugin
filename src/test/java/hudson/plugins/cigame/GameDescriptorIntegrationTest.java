package hudson.plugins.cigame;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;

public class GameDescriptorIntegrationTest extends HudsonTestCase {

    public void testThatSettingCaseInsensitiveFlagWorks() throws Exception {
        GameDescriptor descriptor = hudson.getDescriptorByType(GameDescriptor.class);
        assertThat(descriptor.getSuccessfulBuildPoints(), is(1));
        WebClient webClient = new WebClient();
        webClient.setThrowExceptionOnScriptError(false);
        
        HtmlForm form = webClient.goTo("configure").getFormByName("config");
        assertThat(form.getInputByName("_.successfulBuildPoints").asText(), is("1"));
        form.getInputByName("_.successfulBuildPoints").setValueAttribute("2");
        form.submit((HtmlButton)last(form.getHtmlElementsByTagName("button")));
        
        
        form = webClient.goTo("configure").getFormByName("config");
        assertThat(form.getInputByName("_.successfulBuildPoints").asText(), is("2"));
        form.getInputByName("_.successfulBuildPoints").setValueAttribute("1");
        form.submit((HtmlButton)last(form.getHtmlElementsByTagName("button")));
        
    }

    @LocalData
    public void testLoadingCaseInsensitiveFlagWorks() throws Exception {
        GameDescriptor descriptor = hudson.getDescriptorByType(GameDescriptor.class);
        assertThat(descriptor.getSuccessfulBuildPoints(), is(1));
        HtmlForm form = new WebClient().goTo("configure").getFormByName("config");
        
    }
}
