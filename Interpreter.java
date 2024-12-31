import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpreter provides methods to read a Python file, tokenize it (lexer),
 * and produce a list of lines where each line is a list of tokens.
 */
public class Interpreter {

    /**
     * Reads an entire file at filePath and converts it into tokens grouped by line.
     * @param filePath The absolute or relative path to the Python file.
     * @return A list of lines, where each line is a list of tokens.
     */
    public static List<List<Token>> parse(String filePath) {
        List<List<Token>> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder content = new StringBuilder();
            String line;

            // Read the file line by line and build a single string with newlines.
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            // Convert that big string into tokens.
            return lexer(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return lines;
        }
    }

    /**
     * Converts raw Python code into a list of token-lists, one list per line.
     * @param text The raw Python code as a single string.
     * @return A list of lines, each line being a list of Token objects.
     */
    public static List<List<Token>> lexer(String text) {
        // Split the text by newline characters.
        String[] rawLines = text.split("\n");
        List<List<Token>> tokenLines = new ArrayList<>();

        for (String l : rawLines) {
            int indent = 0;

            // Count leading spaces/tabs to determine indentation level.
            while (indent < l.length() && (l.charAt(indent) == ' ' || l.charAt(indent) == '\t')) {
                indent++;
            }

            // Remove leading indentation to get the main text.
            String trimmed = l.substring(indent);

            // If the trimmed line is empty, skip it.
            if (trimmed.isEmpty()) {
                continue;
            }

            List<Token> lineTokens = new ArrayList<>();
            char[] arr = trimmed.toCharArray();
            StringBuilder temp = new StringBuilder();
            boolean inQuotes = false;
            int quoteCount = 0;

            // Scan each character to split tokens by space or special characters.
            for (char c : arr) {
                // Toggle 'inQuotes' state when encountering a quote char.
                if (c == '"' || c == '\'') {
                    quoteCount++;
                    inQuotes = (quoteCount % 2 != 0);
                }

                // If not inside quotes and the character is space or a separator ((),:{}[]),
                // then we finalize the current token (temp) and also add the separator as a token.
                if (!inQuotes && (c == ' ' || "(),:{}[]".indexOf(c) >= 0)) {
                    if (!temp.isEmpty()) {
                        lineTokens.add(new Token(temp.toString(), indent));
                        temp.setLength(0);
                    }
                    if (c != ' ') {
                        // The separator itself is also a token.
                        lineTokens.add(new Token(String.valueOf(c), indent));
                    }
                } else {
                    // Otherwise, keep building the current token.
                    temp.append(c);
                }
            }

            // If there's any leftover text in 'temp', add it as a token.
            if (!temp.isEmpty()) {
                lineTokens.add(new Token(temp.toString(), indent));
            }

            // If after all this, we have tokens, add them to tokenLines.
            if (!lineTokens.isEmpty()) {
                tokenLines.add(lineTokens);
            }
        }

        return tokenLines;
    }

    /**
     * Token holds the string value and indentation level for each piece of Python code.
     */
    public static class Token {
        public String value;
        public int indentLevel;

        /**
         * Constructs a Token with the given string and indent level.
         * @param v     The value of this token (e.g. 'if', '(', 'varName').
         * @param indent The indentation level derived from leading spaces/tabs.
         */
        public Token(String v, int indent) {
            value = v;
            indentLevel = indent;

        }
    }
}
