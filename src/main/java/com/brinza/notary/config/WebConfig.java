package com.brinza.notary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        return new PathLocaleResolver();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/" + PathLocaleResolver.DEFAULT_LOCALE.getLanguage())
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        registry.addViewController("/admin/login").setViewName("admin/login");
    }

    /**
     * Rewrites the {@code @{/css/...}} / {@code @{/images/...}} links in the templates to the
     * content-hashed URLs the version resolver below serves. Without it the hashed files would
     * exist but nothing would ever link to them.
     */
    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofDays(7)).cachePublic();
        // Assets are cached hard for a week, so they are served under a content hash
        // (/css/style-<md5>.css): editing a file changes its URL, which is what lets an edit
        // reach a browser (or a visitor) that is still holding the previous week's copy.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(cacheControl)
                .resourceChain(true)
                .addResolver(contentVersionResolver());
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(cacheControl)
                .resourceChain(true)
                .addResolver(contentVersionResolver());
    }

    private VersionResourceResolver contentVersionResolver() {
        return new VersionResourceResolver().addContentVersionStrategy("/**");
    }
}
