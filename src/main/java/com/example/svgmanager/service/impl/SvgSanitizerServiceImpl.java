package com.example.svgmanager.service.impl;

import com.example.svgmanager.exception.InvalidSvgException;
import com.example.svgmanager.service.SvgSanitizerService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SvgSanitizerServiceImpl implements SvgSanitizerService {

    private static final Logger log = LoggerFactory.getLogger(SvgSanitizerServiceImpl.class);

    private static final Set<String> FORBIDDEN_TAGS = new HashSet<>(Arrays.asList(
            "script", "iframe", "object", "embed", "applet", "frame", "frameset",
            "meta", "link", "base", "form", "input", "button", "textarea", "select"
    ));

    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN =
            Pattern.compile("(?i)^\\s*(javascript|data|vbscript)\\s*:.*");

    private static final Pattern CSS_EXPRESSION_PATTERN =
            Pattern.compile("(?i)(expression|url\\s*\\(\\s*['\"]?\\s*javascript|behavior|vbscript)");

    private static final Pattern MALICIOUS_XXE_PATTERNS = Pattern.compile(
            "(?i)(" +
            "<!ENTITY\\s+[^>]*\\bSYSTEM\\b|" +
            "<!ENTITY\\s+[^>]*\\bPUBLIC\\b\\s*[\"'][^\"']*[\"']\\s*[\"'](file:|http:|https:|ftp:|gopher:|php:|expect:|data:)|" +
            "<!ENTITY\\s+%|" +
            "<!DOCTYPE\\s+[^>]*\\bSYSTEM\\b|" +
            "%[a-zA-Z0-9_\\-\\.]+;|" +
            "\\bSYSTEM\\s+[\"'](file:|http:|https:|ftp:|gopher:|php:|expect:)" +
            ")"
    );

    @Override
    public byte[] sanitizeAndValidateSvg(byte[] rawSvgBytes) {
        if (rawSvgBytes == null || rawSvgBytes.length == 0) {
            throw new InvalidSvgException("SVG file content is empty");
        }

        String svgContent = new String(rawSvgBytes, StandardCharsets.UTF_8);

        validateAgainstXxe(svgContent);

        Document doc = Jsoup.parse(svgContent, "", Parser.xmlParser());
        doc.outputSettings().prettyPrint(false);

        Element root = doc.selectFirst("svg");
        if (root == null) {
            Elements allElements = doc.children();
            Element svgCandidate = allElements.stream()
                    .filter(el -> el.tagName().equalsIgnoreCase("svg"))
                    .findFirst()
                    .orElse(null);
            if (svgCandidate == null) {
                throw new InvalidSvgException("Uploaded file does not contain a valid <svg> root element");
            }
            root = svgCandidate;
        }

        sanitizeElementTree(root);

        String sanitizedXml = root.outerHtml();

        if (!sanitizedXml.trim().startsWith("<svg")) {
            throw new InvalidSvgException("Invalid SVG structure after sanitization");
        }

        return sanitizedXml.getBytes(StandardCharsets.UTF_8);
    }

    private void validateAgainstXxe(String content) {
        if (isMaliciousXxe(content)) {
            log.warn("Malicious XXE entity pattern detected in SVG content");
            throw new InvalidSvgException("SVG contains prohibited DOCTYPE or Entity definitions (XXE protection)");
        }
    }

    private boolean isMaliciousXxe(String content) {
        if (content == null) {
            return false;
        }
        return MALICIOUS_XXE_PATTERNS.matcher(content).find();
    }

    private void sanitizeElementTree(Element element) {
        String tagName = element.tagName().toLowerCase(Locale.ROOT);
        if (FORBIDDEN_TAGS.contains(tagName)) {
            log.warn("Removing forbidden tag from SVG: <{}>", tagName);
            element.remove();
            return;
        }

        Set<String> attributesToRemove = new HashSet<>();
        for (Attribute attr : element.attributes()) {
            String key = attr.getKey().toLowerCase(Locale.ROOT);
            String val = attr.getValue();

            if (key.startsWith("on")) {
                log.warn("Removing event listener attribute '{}' from <{}>", key, tagName);
                attributesToRemove.add(attr.getKey());
                continue;
            }

            if (key.endsWith("href") || key.equals("src") || key.equals("action")) {
                if (JAVASCRIPT_PROTOCOL_PATTERN.matcher(val).find()) {
                    log.warn("Removing unsafe protocol in attribute '{}': {}", key, val);
                    attributesToRemove.add(attr.getKey());
                    continue;
                }
            }

            if (key.equals("style")) {
                if (CSS_EXPRESSION_PATTERN.matcher(val).find()) {
                    log.warn("Removing unsafe style attribute content: {}", val);
                    attributesToRemove.add(attr.getKey());
                }
            }
        }

        for (String attrKey : attributesToRemove) {
            element.removeAttr(attrKey);
        }

        Elements children = element.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            sanitizeElementTree(children.get(i));
        }
    }
}
