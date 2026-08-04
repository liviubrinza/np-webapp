package com.brinza.notary.controller.web;

import com.brinza.notary.config.PathLocaleResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Locale;

/**
 * Serves sitemap.xml and robots.txt, generated from the same public page list and base URL used
 * for canonical/hreflang tags, so they can never drift out of sync with the actual routes.
 */
@Controller
public class SeoController {

    private static final List<String> PUBLIC_PAGE_PATHS = List.of("", "/services", "/contact", "/book");

    private final String baseUrl;

    public SeoController(@Value("${app.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (Locale locale : PathLocaleResolver.SUPPORTED_LOCALES) {
            for (String path : PUBLIC_PAGE_PATHS) {
                xml.append("  <url><loc>")
                        .append(escapeXml(baseUrl + '/' + locale.getLanguage() + path))
                        .append("</loc></url>\n");
            }
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots() {
        return "User-agent: *\n"
                + "Disallow: /admin/\n"
                + "Disallow: /h2-console/\n"
                + "\n"
                + "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
