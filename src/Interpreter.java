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
            StringBuilder tempStr = new StringBuilder();
            List<Token> tokens = new ArrayList<>();
            int quoteCount = 0;
            boolean inQuotes = false;

            for (char c : chars) {
                if (c == '"' || c == '\'') {
                    quoteCount++;
                    inQuotes = quoteCount % 2 != 0;
                }
                if (c == ' ' && !inQuotes) {
                    tokens.add(new Token(tempStr.toString()));
                    tempStr = new StringBuilder();
                } else {
                    tempStr.append(c);
                }
            }

            tokens.add(new Token(tempStr.toString()));
            nLines.add(tokens);
        }
        return nLines;
    }

    public static List<List<Token>> parse(String file) {
        List<List<Token>> lines = new ArrayList<>();
        StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lexer(contents.toString());
    }

    public static void exec(String instLine) {
        // Placeholder for exec implementation
    }

    public static class Token {
        String type;
        String value;

        Token(String value) {
            this.value = value;
            determineType();
        }

        private void determineType() {
            if (value.matches("[\"'].*[\"']")) {
                this.type = "String";
            } else if (Pattern.matches("[.a-zA-Z]+", value)) {
                this.type = "Symbol";
            } else if ("+-*/=".contains(value)) {
                this.type = "Expression";
            } else if (Pattern.matches("[.0-9]+", value)) {
                this.type = "Number";
            }
            else if("print".equals(value)){
                this.type="print";
            }
            else if ("(".equals(value)) {
                this.type = "LeftParenthesis";
            } else if (")".equals(value)) {
                this.type = "RightParenthesis";
            }
        }

        @Override
        public String toString() {
            return type + ": " + value;
        }
    }

}
