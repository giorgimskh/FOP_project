import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class Interpreter {
    static HashMap<String, String> Vars = new HashMap<>();
    static String[] Symbols = {"var", "function", "print"};

    public static List<List<Token>> lexer(String contents) {
        String[] lines = contents.split("\n");
        List<List<Token>> nLines = new ArrayList<>();

        for (String line : lines) {
            char[] chars = line.toCharArray();
            List<Token> tokens = new ArrayList<>();
            StringBuilder tempStr = new StringBuilder();
            boolean inQuotes = false;
            int quoteCount = 0;

            for (char c : chars) {
                if (c == '"' || c == '\'') {
                    quoteCount++;
                    inQuotes = quoteCount % 2 != 0;
                }
                if (!inQuotes && (c == ' ' || "(),:{}[]".indexOf(c) >= 0)) {
                    if (tempStr.length() > 0) {
                        tokens.add(new Token(tempStr.toString()));
                        tempStr.setLength(0);
                    }
                    if (c != ' ') {
                        tokens.add(new Token(String.valueOf(c)));
                    }
                } else {
                    tempStr.append(c);
                }
            }
            if (tempStr.length() > 0) {
                tokens.add(new Token(tempStr.toString()));
            }
            nLines.add(tokens);
        }
        return nLines;
    }

    public static List<List<Token>> parse(String file) {
        List<List<Token>> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder contents = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
            return lexer(contents.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return lines; // In case of IOException, return an empty list of tokens.
        }
    }

    public static class Token {
        String type;
        String value;

        public Token(String value) {
            this.value = value;
            determineType();
        }

        private void determineType() {
            if (value.matches("[\"'].*[\"']")) {
                type = "string";
            } else if (Pattern.matches("[.a-zA-Z]+", value)) {
                type = "symbol";
            } else if ("+-*/=,{}()[]:".contains(value)) {
                type = "operator";
            } else if (Pattern.matches("[.0-9]+", value)) {
                type = "number";
            } else {
                type = "unknown";
            }
        }

        @Override
        public String toString() {
            return type + ": " + value;
        }
    }
}
