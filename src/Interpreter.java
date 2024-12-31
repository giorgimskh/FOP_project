import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Interpreter {
    public static List<List<Token>> parse(String filePath) {
        List<List<Token>> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return lexer(content.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return lines;
        }
    }

    public static List<List<Token>> lexer(String text) {
        String[] rawLines = text.split("\n");
        List<List<Token>> tokenLines = new ArrayList<>();
        for (String l : rawLines) {
            int indent = 0;
            while (indent < l.length() && (l.charAt(indent) == ' ' || l.charAt(indent) == '\t')) {
                indent++;
            }
            String trimmed = l.substring(indent);
            if (trimmed.isEmpty()) {
                continue;
            }
            List<Token> lineTokens = new ArrayList<>();
            char[] arr = trimmed.toCharArray();
            StringBuilder temp = new StringBuilder();
            boolean inQuotes = false;
            int quoteCount = 0;
            for (char c : arr) {
                if (c == '"' || c == '\'') {
                    quoteCount++;
                    inQuotes = (quoteCount % 2 != 0);
                }
                if (!inQuotes && (c == ' ' || "(),:{}[]".indexOf(c) >= 0)) {
                    if (temp.length() > 0) {
                        lineTokens.add(new Token(temp.toString(), indent));
                        temp.setLength(0);
                    }
                    if (c != ' ') {
                        lineTokens.add(new Token(String.valueOf(c), indent));
                    }
                } else {
                    temp.append(c);
                }
            }
            if (temp.length() > 0) {
                lineTokens.add(new Token(temp.toString(), indent));
            }
            if (!lineTokens.isEmpty()) {
                tokenLines.add(lineTokens);
            }
        }
        return tokenLines;
    }

    public static class Token {
        public String value;
        public int indentLevel;
        public Token(String v, int indent) {
            value = v;
            indentLevel = indent;
            determineType();
        }
        private void determineType() {
            if (Pattern.matches("[\"'].*[\"']", value)) {
            } else if (Pattern.matches("[.a-zA-Z]+", value)) {
            } else if ("+-*/=,{}()[]:%".contains(value)) {
            } else if (Pattern.matches("[.0-9]+", value)) {
            }
        }
    }
}
