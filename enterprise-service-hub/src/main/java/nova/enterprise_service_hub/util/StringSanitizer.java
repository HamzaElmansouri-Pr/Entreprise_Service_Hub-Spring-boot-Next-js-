package nova.enterprise_service_hub.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * OWASP-aligned String Sanitizer — Strips dangerous HTML/JS from user input.
 * <p>
 * Uses jsoup's {@link Safelist} to neutralize XSS payloads while preserving
 * safe text content. Applied to all long-text fields before database
 * persistence.
 *
 * <h3>Examples:</h3>
 * 
 * <pre>
 *   "&lt;script&gt;alert(1)&lt;/script&gt;Hello"  → "Hello"
 *   "&lt;b&gt;Bold&lt;/b&gt; text"                → "Bold text"       (NONE mode)
 *   "&lt;b&gt;Bold&lt;/b&gt; text"                → "&lt;b&gt;Bold&lt;/b&gt; text"  (BASIC mode)
 * </pre>
 */
public final class StringSanitizer {

    private StringSanitizer() {
        // Utility class — no instantiation
    }

    /**
     * Strips ALL HTML tags, leaving only plain text.
     * Use for: descriptions, case study text, names.
     */
    public static String stripAll(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return Jsoup.clean(input, Safelist.none()).trim();
    }

    /**
     * Allows basic formatting tags (b, i, u, em, strong, p, br, ul, ol, li).
     * Use for: rich-text fields where basic formatting is acceptable.
     */
    public static String stripDangerous(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        return Jsoup.clean(input, Safelist.basic()).trim();
    }

    /**
     * Checks if a string contains any HTML/script tags.
     */
    public static boolean containsHtml(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String cleaned = Jsoup.clean(input, Safelist.none());
        return !cleaned.equals(input);
    }
}
