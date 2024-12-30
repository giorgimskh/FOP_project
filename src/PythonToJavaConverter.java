import java.util.List;

public class PythonToJavaConverter {
    private List<List<Interpreter.Token>> tokens;

    public PythonToJavaConverter(List<List<Interpreter.Token>> tokens) {
        this.tokens = tokens;
    }

    public String convert() {
        StringBuilder javaCode = new StringBuilder();
        boolean inForLoop = false; // To track if we are currently processing a for-loop block
        int currentDepth = 0; // Current depth based on tab indentation

        for (int i = 0; i < tokens.size(); i++) {
            List<Interpreter.Token> lineTokens = tokens.get(i);
            if (lineTokens.isEmpty()) continue;

            if ("for".equals(lineTokens.get(0).value)) {
                inForLoop = true; // Enter for-loop context
                currentDepth = countTabs(lineTokens); // Set current depth based on this line
                javaCode.append(parseForLoop(lineTokens)); // Parse the initial for-loop line
            } else if (inForLoop) {
                int lineDepth = countTabs(lineTokens);
                if (lineDepth >= currentDepth) {
                    // If deeper, this is part of the for-loop body
                    javaCode.append("\t").append(translatePythonLineToJava(lineTokens)).append("\n");
                } else {
                    // If not deeper or same, end the current for-loop
                    javaCode.append("}\n"); // Close the for-loop block
                    inForLoop = false;
                    i--; // Re-evaluate this line as it might be a new block or statement
                }
            } else {
                javaCode.append(translatePythonLineToJava(lineTokens)).append("\n");
            }
        }

        if (inForLoop) {
            javaCode.append("}\n"); // Ensure the for-loop is closed if the file ends
        }

        return javaCode.toString();
    }

    private int countTabs(List<Interpreter.Token> tokens) {
        // Count leading tabs or any indentation-specific tokens
        String line = tokens.stream().map(t -> t.value).reduce("", String::concat);
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '\t') count++; // Count tabs
            else break;
        }
        return count;
    }

    private String parseForLoop(List<Interpreter.Token> tokens) {
        String var = tokens.get(1).value;
        int start = Integer.parseInt(tokens.get(5).value);
        String end = tokens.get(7).value;
        return String.format("for (int %s = %d; %s < %s; %s++) {\n", var, start, var, end, var);
    }

    private String translatePythonLineToJava(List<Interpreter.Token> tokens) {
        // This method should handle converting individual Python statements to Java
        StringBuilder javaLine = new StringBuilder();
        // Example of translation logic:
        for (Interpreter.Token token : tokens) {
            // Append or convert each token based on type and content
            javaLine.append(token.value); // Placeholder logic
        }
        return javaLine.toString();
    }
}
