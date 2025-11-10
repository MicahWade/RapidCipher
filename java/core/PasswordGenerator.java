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

    private static List<String> WORD_LIST;

    private static final SecureRandom random = new SecureRandom();

    static {
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
// Backup Word List
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

    public static String generatePassword(int length, boolean useUpper, boolean useDigits, boolean useSymbols, int minDigits, int minSymbols) {
        
        int minLower = 1;
        int minUpper = useUpper ? 1 : 0;
        
        int reqDigits = useDigits ? minDigits : 0;
        int reqSymbols = useSymbols ? minSymbols : 0;
        
        int totalMin = minLower + minUpper + reqDigits + reqSymbols;
        
        if(length < totalMin) {
            length = totalMin;
        }

        List<Character> passChars = new ArrayList<>();
        String allChars = LOWER;
        
        for(int i = 0; i < minLower; i++) passChars.add(LOWER.charAt(random.nextInt(LOWER.length())));
        
        if (useUpper) {
            allChars += UPPER;
            for(int i = 0; i < minUpper; i++) passChars.add(UPPER.charAt(random.nextInt(UPPER.length())));
        }
        
        if (useDigits) {
            allChars += DIGITS;
            for (int i = 0; i < reqDigits; i++) {
                passChars.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
            }
        }
        
        if (useSymbols) {
            allChars += SYMBOLS;
            for (int i = 0; i < reqSymbols; i++) {
                passChars.add(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
            }
        }

        for (int i = passChars.size(); i < length; i++) {
            passChars.add(allChars.charAt(random.nextInt(allChars.length())));
        }

        Collections.shuffle(passChars, random);
        return passChars.stream()
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
            passphrase.append(WORD_LIST.get(random.nextInt(WORD_LIST.size())));
            if (i < numWords - 1) {
                passphrase.append(separator);
            }
        }
        return passphrase.toString();
    }
}