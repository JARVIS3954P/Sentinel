package org.jarvis.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainScraper {

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "([a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]\\.)+[a-zA-Z]{2,}"
    );

    // A blacklist of common, irrelevant domains found in search results.
    private static final Set<String> DOMAIN_BLACKLIST = Set.of(
            "duckduckgo.com", "duck.com", "donttrack.us", "google.com",
            "youtube.com", "facebook.com", "instagram.com", "wikipedia.org",
            "twitter.com", "linkedin.com", "github.com", "w3.org", "archive.org"
    );

    public Set<String> findRelatedDomains(String keyword) {
        Set<String> foundDomains = new HashSet<>();
        try {
            // A more specific query to find service and CDN domains.
            String query = "all domains and subdomains used by " + keyword + " for content delivery";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            URL url = new URL("https://html.duckduckgo.com/html/?q=" + encodedQuery);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
            connection.setRequestMethod("GET");

            StringBuilder htmlContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    htmlContent.append(line);
                }
            }

            Matcher matcher = DOMAIN_PATTERN.matcher(htmlContent.toString());
            while (matcher.find()) {
                String domain = matcher.group().toLowerCase();

                // Add any discovered domain that contains the keyword and is not on our ignore list.
                if (!DOMAIN_BLACKLIST.contains(domain) && domain.contains(keyword)) {
                    foundDomains.add(domain);
                }
            }

        } catch (Exception e) {
            System.err.println("An error occurred during domain scraping.");
            e.printStackTrace();
        }

        // Always add the base domain itself, as scraping might miss it.
        foundDomains.add(keyword + ".com");
        foundDomains.add("www." + keyword + ".com");

        return foundDomains;
    }
}