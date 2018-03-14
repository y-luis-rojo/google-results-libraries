package com.scalable;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Web crawler script to extract the top most used libraries from Google search results.
 * Improvements to this algorithm can be done by processing links in parallel and using a thread safe data structure like ConcurrentMap
 */
public class WebCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebCrawler.class);

    public static void main(String[] args) {

        if (args.length == 0) {
            LOGGER.error("Required search term not set");
            return;
        }

        String url = String.format("https://www.google.com/search?q=%s", args[0]);

        try {
            //Support a second argument, if exists (and Integer) that will be used for result size
            if (args.length > 1)
                process(url, Integer.parseInt(args[1]));
            else
                process(url);

        } catch (NumberFormatException e) {
            LOGGER.error("Limit argument not valid");
        }
    }

    /**
     * Crawl given root URL and print top 5
     *
     * @param root URL
     */
    private static void process(String root) {
        process(root, 5);
    }


    /**
     * Crawl given root URL and print top limit
     *
     * @param root  URL
     * @param limit top limit
     */
    private static void process(String root, int limit) {
        try {
            //Use of Jsoup for HTML parsing
            Document doc = Jsoup.connect(root).get();

            //Google result links are inside divs with class "r"
            Elements links = doc.select(".r a");

            //Use a Map for efficiently store library names and occurrences
            Map<String, Integer> libraries = new HashMap<>();

            for (Element link : links) {
                String href = link.absUrl("href");

                LOGGER.debug("Downloading {}{}", href, "...");

                try {
                    Document page = Jsoup.connect(href).get();

                    Elements scripts = page.select("script");

                    for (Element script : scripts) {

                        String src = script.absUrl("src");

                        //Count only external scripts
                        if (!src.isEmpty()) {
                            URL url = new URL(src);

                            //Parse library (file) name
                            String library = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);

                            //Increment value if exists, 1 otherwise
                            libraries.merge(library, 1, (old, one) -> old + one);
                        }
                    }
                } catch (HttpStatusException e) {
                    LOGGER.warn("Could not fetch page from {}", href);
                }
            }

            LOGGER.info("{} -- {}", "Libraries", "Occurrences");

            //Java 8 stream to sort Map entries by values
            libraries.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(limit).forEach(kv -> LOGGER.info("{} -- {}", kv.getKey(), kv.getValue()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
