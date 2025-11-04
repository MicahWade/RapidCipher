package core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    // This will be populated from the resource file
    private static List<String> WORD_LIST;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Static initializer block to load the wordlist from resources.
     * This code runs once when the class is first loaded.
     */
    static {
        // The path must start with '/' to search from the root of the classpath
        String resourcePath = "/top_english_adjs_lower_10000.txt";
        
        try (InputStream is = PasswordGenerator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Wordlist file not found in resources: " + resourcePath);
            }
            
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                
                WORD_LIST = reader.lines().collect(Collectors.toList());
                
                if (WORD_LIST.isEmpty()) {
                     throw new RuntimeException("Wordlist file is empty.");
                }
                
                System.out.println("Successfully loaded wordlist with " + WORD_LIST.size() + " words.");
            }
        } catch (Exception e) {
            System.err.println("!!! FAILED TO LOAD WORDLIST from resources. Falling back to small, hard-coded list. !!!");
            System.err.println("Error: " + e.getMessage());
            
            // Fallback to the original small list if loading fails
            WORD_LIST = List.of(
                "acid", "acorn", "acre", "acts", "afar", "affix", "aged", "agent",
                "agile", "aging", "agony", "ahead", "aide", "aids", "aim", "air",
                "aisle", "ajar", "alarm", "album", "alert", "algae", "alias", "alibi",
                "alien", "alike", "alive", "alloy", "ally", "almond", "aloe", "along",
                "aloof", "aloud", "alpha", "altar", "alter", "always", "amaze", "amber",
                "ambush", "amend", "ample", "amuse", "anchor", "angel", "anger", "angle",
                "ankle", "annoy", "annual", "answer", "anthem", "any", "apart", "apex",
                "aphid", "apple", "apply", "apron", "apt", "aqua", "arcade", "archer",
                "area", "arena", "argue", "arise", "arm", "armful", "army", "aroma",
                "around", "arrow", "arson", "art", "ascend", "ash", "asleep", "aspect",
                "assay", "asset", "atlas", "atom", "attic", "audio", "audit", "aunt",
                "aura", "auto", "autumn", "avatar", "avid", "avoid", "awake", "award",
                "aware", "awful", "axis", "bacon", "badge", "badly", "bag", "baggy",
                "bake", "balance", "balcony", "ball", "band", "banjo", "bank", "bar",
                "barge", "barn", "base", "bash", "basic", "basin", "basket", "bat"
            );
        }
    }

    public static String generatePassword(int length, boolean useUpper, boolean useDigits, boolean useSymbols) {
        if (length < 4) {
            length = 4;
        }

        List<Character> charCategories = new ArrayList<>();
        charCategories.add(LOWER.charAt(random.nextInt(LOWER.length())));
        
        String allChars = LOWER;

        if (useUpper) {
            charCategories.add(UPPER.charAt(random.nextInt(UPPER.length())));
            allChars += UPPER;
        }
        if (useDigits) {
            charCategories.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
            allChars += DIGITS;
        }
        if (useSymbols) {
            charCategories.add(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
            allChars += SYMBOLS;
        }

        for (int i = charCategories.size(); i < length; i++) {
            charCategories.add(allChars.charAt(random.nextInt(allChars.length())));
        }

        Collections.shuffle(charCategories, random);
        return charCategories.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    public static String generatePassphrase(int numWords, String separator) {
        if (numWords < 3) {
            numWords = 3;
        }
        if (separator == null || separator.isEmpty()) {
            separator = "-";
        }
        
        if (WORD_LIST == null || WORD_LIST.isEmpty()) {
            System.err.println("Wordlist is not loaded or is empty. Cannot generate passphrase.");
            return "!!WORDLIST-ERROR!!";
        }

        StringBuilder passphrase = new StringBuilder();
        for (int i = 0; i < numWords; i++) {
            // Updated to use List.get() and List.size()
            passphrase.append(WORD_LIST.get(random.nextInt(WORD_LIST.size())));
            if (i < numWords - 1) {
                passphrase.append(separator);
            }
        }
        return passphrase.toString();
    }
}