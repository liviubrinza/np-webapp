package com.brinza.notary.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code .hero-section} (style.css) shows a landscape background on wide/landscape viewports and
 * switches to a dedicated portrait-cropped image via {@code @media (orientation: portrait)} on
 * viewports taller than they are wide (phones, or a desktop window resized narrow) — see the
 * comment above that media query in style.css for why a plain {@code cover}/{@code contain} swap
 * on the same image wasn't enough.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HeroBackgroundResponsiveTest {

    private static final Pattern VERSIONED_CSS = Pattern.compile("/css/style-([0-9a-f]{32})\\.css");
    private static final Pattern PORTRAIT_MEDIA_BLOCK =
            Pattern.compile("@media \\(orientation: portrait\\) \\{([\\s\\S]*?)\\n}");

    // The resource chain's CssLinkResourceTransformer rewrites url(...) references inside CSS to
    // the same content-hashed filenames it serves the images under, so the served stylesheet never
    // contains the literal "background.png" - only "background-<hash>.png".
    private static final Pattern LANDSCAPE_BACKGROUND_RULE = Pattern.compile(
            "url\\('/images/background(?:-[0-9a-f]{32})?\\.png'\\) center center / contain no-repeat");
    private static final Pattern MOBILE_BACKGROUND_RULE = Pattern.compile(
            "url\\('/images/background_mobile(?:-[0-9a-f]{32})?\\.png'\\) center center / cover no-repeat");

    @Autowired
    private MockMvc mockMvc;

    private String fetchStylesheet() throws Exception {
        String html = mockMvc.perform(get("/admin/login")).andReturn().getResponse().getContentAsString();
        Matcher matcher = VERSIONED_CSS.matcher(html);
        assertThat(matcher.find()).as("versioned stylesheet link in %s", html).isTrue();

        return mockMvc.perform(get(matcher.group()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void defaultHeroBackgroundUsesTheLandscapeImageWithContain() throws Exception {
        String css = fetchStylesheet();

        String beforeMediaQuery = css.substring(0, css.indexOf("@media (orientation: portrait)"));
        assertThat(beforeMediaQuery).contains(".hero-section {");
        assertThat(LANDSCAPE_BACKGROUND_RULE.matcher(beforeMediaQuery).find())
                .as("landscape background rule in %s", beforeMediaQuery)
                .isTrue();
    }

    @Test
    void portraitViewportsSwitchToTheMobileImageWithCover() throws Exception {
        String css = fetchStylesheet();

        Matcher matcher = PORTRAIT_MEDIA_BLOCK.matcher(css);
        assertThat(matcher.find()).as("an @media (orientation: portrait) block in %s", css).isTrue();

        String mediaBlock = matcher.group(1);
        assertThat(mediaBlock).contains(".hero-section {");
        assertThat(MOBILE_BACKGROUND_RULE.matcher(mediaBlock).find())
                .as("mobile background rule in %s", mediaBlock)
                .isTrue();
    }

    @Test
    void bothHeroBackgroundImagesAreServable() throws Exception {
        mockMvc.perform(get("/images/background.png")).andExpect(status().isOk());
        mockMvc.perform(get("/images/background_mobile.png")).andExpect(status().isOk());
    }
}
