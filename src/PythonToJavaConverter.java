import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * PythonToJavaConverter takes a list of tokenized Python lines and converts
 * them into Java code. It handles if/elif/else blocks, for/while loops,
 * variable declarations, and more.
 */
public class PythonToJavaConverter {

    // Each element in 'tokens' is a list of tokens that corresponds to a single line of Python code.
    private final List<List<Interpreter.Token>> tokens;

    // Maps variable names to their inferred types (e.g., "x" -> "int").
    private final HashMap<String, String> varTypes = new HashMap<>();

    // Stack of indentation levels (one per open block).
    private final Stack<Integer> blockIndents = new Stack<>();

    // Stack of block types ("if", "for", "while"), corresponding to the indentation stack.
    private final Stack<String> blockTypes = new Stack<>();

    /**
     * Constructs a PythonToJavaConverter with a list of lines (each line is a list of tokens).
     * @param lines The tokenized Python code, line by line.
     */
    public PythonToJavaConverter(List<List<Interpreter.Token>> lines) {
        this.tokens = lines;
    }

    /**
     * Main method to convert all stored tokens to a string of Java code.
     * @return A Java code snippet (without class declaration), including
     *         statements, loops, conditionals, etc.
     */
    public String convert() {
        StringBuilder code = new StringBuilder();

        // Iterate over each line of tokenized Python.
        for (List<Interpreter.Token> line : tokens) {
            // Skip empty lines.
            if (line.isEmpty()) {
                continue;
            }

            // Python indentation level for this line.
            int lineIndent = line.getFirst().indentLevel;

            // The first token (e.g. 'if', 'for', or a variable).
            String firstTok = line.getFirst().value;

            // Close any blocks that have indentation >= this line's indentation.
            closeBlocksIfNeeded(lineIndent, code);

            // Check the first token to determine type of statement.
            switch (firstTok) {
                case "if" -> openIfBlock(line, lineIndent, code);
                case "elif" -> {
                    // Transition from if -> elif at the same indent level.
                    closeOneBlockIfSameIndent(lineIndent, code, "if");
                    code.append("else if (")
                            .append(parseCondition(line))
                            .append(") {\n");
                    blockIndents.push(lineIndent);
                    blockTypes.push("if");
                }
                case "else" -> {
                    // Transition from if/elif -> else at the same indent level.
                    closeOneBlockIfSameIndent(lineIndent, code, "if");
                    code.append("else {\n");
                    blockIndents.push(lineIndent);
                    blockTypes.push("if");
                }
                case "for" -> openForBlock(line, lineIndent, code);
                case "while" -> openWhileBlock(line, lineIndent, code);
                case null, default ->
                    // Normal statements like assignment, print, break, continue, etc.
                        code.append(translateLine(line)).append("\n");
            }
        }

        // After processing all lines, close any remaining open blocks.
        while (!blockIndents.isEmpty()) {
            code.append("}\n");
            blockIndents.pop();
            blockTypes.pop();
        }

        return code.toString();
    }

    /**
     * Closes blocks whose indentation level is >= the current line's indent.
     * This matches Python's way of ending blocks when you de-indent.
     */
    private void closeBlocksIfNeeded(int lineIndent, StringBuilder code) {
        while (!blockIndents.isEmpty() && blockIndents.peek() >= lineIndent) {
            code.append("}\n");
            blockIndents.pop();
            blockTypes.pop();
        }
    }

    /**
     * Closes exactly one block if the current top block has the same
     * indentation level and matches a specific type (e.g., "if").
     * Used for transitioning if->elif->else at the same indentation.
     */
    private void closeOneBlockIfSameIndent(int lineIndent, StringBuilder code, String typeNeeded) {
        if (!blockIndents.isEmpty()
                && blockIndents.peek() == lineIndent
                && blockTypes.peek().equals(typeNeeded)) {
            code.append("}\n");
            blockIndents.pop();
            blockTypes.pop();
        }
    }

    /**
     * Opens an if-block.
     */
    private void openIfBlock(List<Interpreter.Token> line, int lineIndent, StringBuilder code) {
        code.append("if (")
                .append(parseCondition(line))
                .append(") {\n");
        blockIndents.push(lineIndent);
        blockTypes.push("if");
    }

    /**
     * Opens a for-loop block by translating 'for var in range(...)' to Java for-loop syntax.
     */
    private void openForBlock(List<Interpreter.Token> line, int lineIndent, StringBuilder code) {
        code.append(parseForLoop(line));
        blockIndents.push(lineIndent);
        blockTypes.push("for");
    }

    /**
     * Opens a while-loop block by translating Python while-syntax to Java while-syntax.
     */
    private void openWhileBlock(List<Interpreter.Token> line, int lineIndent, StringBuilder code) {
        code.append(parseWhile(line));
        blockIndents.push(lineIndent);
        blockTypes.push("while");
    }

    // --------------------- Parsing if, for, while --------------------- //

    /**
     * Extracts the condition from an if/elif statement (everything up to the colon).
     */
    private String parseCondition(List<Interpreter.Token> line) {
        // Skip the first token ('if' or 'elif'), then read until ':'.
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < line.size(); i++) {
            if (":".equals(line.get(i).value)) break;
            sb.append(line.get(i).value);
        }
        // fixExpr() handles things like Python booleans -> Java booleans.
        return fixExpr(sb.toString().trim());
    }

    /**
     * Translates Python's 'for var in range(...)' to a Java for-loop.
     * E.g. for i in range(5) => for (i = 0; i < 5; i++)
     */
    private String parseForLoop(List<Interpreter.Token> line) {
        // The variable name is typically the second token, e.g. 'for i in range(...)'
        String varName = line.get(1).value;

        // If the variable type is unknown, assume int by default.
        if (!varTypes.containsKey(varName)) {
            varTypes.put(varName, "int");
        }

        // Find the positions of '(' and ')' in 'range(...)'.
        int openP = -1, closeP = -1;
        for (int i = 0; i < line.size(); i++) {
            if ("(".equals(line.get(i).value)) {
                openP = i;
                break;
            }
        }
        for (int i = openP + 1; i < line.size(); i++) {
            if (")".equals(line.get(i).value)) {
                closeP = i;
                break;
            }
        }

        // Extract the inside of range(...).
        StringBuilder inside = new StringBuilder();
        for (int i = openP + 1; i < closeP; i++) {
            inside.append(line.get(i).value);
        }
        String rangeText = inside.toString().trim();

        // Python's range can have up to 3 parameters: start, end, step.
        String[] parts = splitTopComma(rangeText);

        String startExpr = "0";  // default start
        String endExpr = null;   // must be discovered
        String stepExpr = "1";   // default step

        // Assign properly based on number of arguments in range(...).
        if (parts.length == 1) {
            endExpr = parts[0];
        } else if (parts.length == 2) {
            startExpr = parts[0];
            endExpr = parts[1];
        } else if (parts.length == 3) {
            startExpr = parts[0];
            endExpr = parts[1];
            stepExpr = parts[2];
        }

        // Convert any Python expressions to Java-friendly equivalents.
        startExpr = fixExpr(startExpr);
        endExpr   = fixExpr(endExpr != null ? endExpr : "0");
        stepExpr  = fixExpr(stepExpr);

        StringBuilder sb = new StringBuilder();

        // If variable is not declared, declare it. If it was declared with a different type, override to int.
        if (varTypes.containsKey(varName)) {
            varTypes.put(varName, "int");
            sb.append("int ").append(varName).append(";\n");
        } else if (!"int".equals(varTypes.get(varName))) {
            varTypes.put(varName, "int");
            sb.append("int ").append(varName).append(";\n");
        }

        // Initialize variable to startExpr, then build the for-loop structure in Java.
        sb.append(varName).append(" = ").append(startExpr).append(";\n")
                .append("for (; ")
                .append(varName).append(" < ").append(endExpr)
                .append("; ");

        // If step = 1, use ++, else use += stepExpr.
        if ("1".equals(stepExpr)) {
            sb.append(varName).append("++");
        } else {
            sb.append(varName).append(" += ").append(stepExpr);
        }
        sb.append(") {\n");

        return sb.toString();
    }

    /**
     * Translates a Python while statement into a Java while.
     */
    private String parseWhile(List<Interpreter.Token> line) {
        // If the form is "while x:", interpret x != 0.
        if (line.size() >= 3 && ":".equals(line.get(2).value)) {
            String var = line.get(1).value;
            if (!varTypes.containsKey(var)) {
                varTypes.put(var, "int");
            }
            return "while (" + var + " != 0) {\n";
        } else {
            // E.g. "while x < 5:" => while (x < 5) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < line.size(); i++) {
                if (":".equals(line.get(i).value)) break;
                sb.append(line.get(i).value);
            }
            return "while (" + fixExpr(sb.toString().trim()) + ") {\n";
        }
    }

    // --------------------- Normal statements (assignments, print, etc.) --------------------- //

    /**
     * Translates a normal Python line (assignments, prints, break, continue, etc.) into Java.
     * @param line Tokens for a single line of Python code.
     * @return A line of Java code (possibly with a semicolon at the end).
     */
    private String translateLine(List<Interpreter.Token> line) {
        StringBuilder sb = new StringBuilder();
        for (Interpreter.Token t : line) {
            sb.append(t.value);
        }
        String raw = sb.toString().trim();

        // Convert Python print(...) to Java System.out.println(...).
        if (raw.startsWith("print(") && raw.endsWith(")")) {
            // Extract everything inside the parentheses.
            String inside = raw.substring("print(".length(), raw.length() - 1);
            return "System.out.println(" + convertPrint(inside) + ");\n \n";
        }

        // If this is an assignment (var = expr), handle type declaration if unknown.
        if (raw.contains("=") && !raw.contains("==")) {
            String[] parts = raw.split("=");
            if (parts.length == 2) {
                String lhs = parts[0].trim();
                String rhs = fixExpr(parts[1].trim());
                // Guess a type for the right-hand side.
                String t = guessType(rhs);

                // If the variable was never used before, declare it with the guessed type.
                if (!varTypes.containsKey(lhs)) {
                    varTypes.put(lhs, t);
                    return t + " " + lhs + " = " + rhs + ";";
                } else {
                    // Otherwise, just do a normal assignment.
                    return lhs + " = " + rhs + ";";
                }
            }
        }

        // If it's 'break' or 'continue', just add a semicolon.
        if ("break".equals(raw) || "continue".equals(raw)) {
            return raw + ";";
        }

        // If it doesn't end with a brace or semicolon, add a semicolon.
        if (!raw.endsWith("{") && !raw.endsWith("}") && !raw.endsWith(";")) {
            raw += ";";
        }
        return raw;
    }

    // --------------------- Utility methods --------------------- //

    /**
     * Converts Python-style print arguments into a single Java expression,
     * concatenating them with '+' if needed.
     */
    private String convertPrint(String inside) {
        String[] arr = inside.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(" + ");
            sb.append(fixExpr(arr[i].trim()));
        }
        return sb.toString();
    }

    /**
     * Replace Python-specific syntax with Java equivalents (e.g., True -> true).
     * Also transforms int(...) calls into Java casts, etc.
     */
    private String fixExpr(String expr) {
        if (expr == null) return "0";
        expr = expr.replace("True", "true")
                .replace("False", "false");
        // Convert 'int(x)' to '(int)(x)'
        expr = expr.replaceAll("\\bint\\((.*?)\\)", "(int)($1)");
        return expr.trim();
    }

    /**
     * Attempts to guess a Java type (int, double, boolean) based on a string expression.
     * Defaults to 'int' if not recognized.
     */
    private String guessType(String x) {
        if ("true".equals(x) || "false".equals(x)) return "boolean";
        if (x.matches("\\d+")) return "int";
        if (x.matches("\\d+\\.\\d+")) return "double";
        if (varTypes.containsKey(x)) return varTypes.get(x);
        return "int";
    }

    /**
     * Splits a string by top-level commas (i.e., commas not nested in parentheses).
     * This is useful for range(...) arguments.
     */
    private String[] splitTopComma(String text) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
                cur.append(c);
            } else if (c == ')') {
                depth--;
                cur.append(c);
            } else if (c == ',' && depth == 0) {
                list.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) {
            list.add(cur.toString());
        }
        return list.toArray(new String[0]);
    }
}
