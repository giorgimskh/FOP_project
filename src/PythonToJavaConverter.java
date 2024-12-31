import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class PythonToJavaConverter {
    private final List<List<Interpreter.Token>> tokens;

    // Keep track of variables -> types (e.g., "number" -> "int")
    private final HashMap<String, String> varTypes = new HashMap<>();

    // Stack to manage open blocks. We store indentation levels
    // and block "types" (e.g., "if", "for", "while") to know
    // how many braces to close as we move among indentation levels.
    private final Stack<Integer> blockIndents = new Stack<>();
    private final Stack<String> blockTypes = new Stack<>();

    public PythonToJavaConverter(List<List<Interpreter.Token>> lines) {
        this.tokens = lines;
    }

    public String convert() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < tokens.size(); i++) {
            List<Interpreter.Token> line = tokens.get(i);
            if (line.isEmpty()) {
                continue;
            }
            int lineIndent = line.get(0).indentLevel;
            String firstTok = line.get(0).value;

            // Close any blocks whose indentation is deeper or equal
            closeBlocksIfNeeded(lineIndent, code);

            // Distinguish statements
            if ("if".equals(firstTok)) {
                openIfBlock(line, lineIndent, code);
            }
            else if ("elif".equals(firstTok)) {
                // Transition from "if" to "elif" at same indent
                closeOneBlockIfSameIndent(lineIndent, code, "if");
                code.append("else if (")
                        .append(parseCondition(line))
                        .append(") {\n");
                blockIndents.push(lineIndent);
                blockTypes.push("if");
            }
            else if ("else".equals(firstTok)) {
                // Transition from "if" or "elif" to "else" at same indent
                closeOneBlockIfSameIndent(lineIndent, code, "if");
                code.append("else {\n");
                blockIndents.push(lineIndent);
                blockTypes.push("if");
            }
            else if ("for".equals(firstTok)) {
                openForBlock(line, lineIndent, code);
            }
            else if ("while".equals(firstTok)) {
                openWhileBlock(line, lineIndent, code);
            }
            else {
                // normal statement (assignment, print, break, etc.)
                code.append(translateLine(line)).append("\n");
            }
        }

        // close all remaining blocks at the end
        while (!blockIndents.isEmpty()) {
            code.append("}\n");
            blockIndents.pop();
            blockTypes.pop();
        }

        return code.toString();
    }

    /**
     * Closes blocks while top block's indent >= new line's indent
     */
    private void closeBlocksIfNeeded(int lineIndent, StringBuilder code) {
        while (!blockIndents.isEmpty() && blockIndents.peek() >= lineIndent) {
            code.append("}\n");
            blockIndents.pop();
            blockTypes.pop();
        }
    }

    /**
     * Closes exactly one block if the top block matches 'typeNeeded'
     * and the indent is the same as lineIndent.  Used for if->elif->else
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

    private void openIfBlock(List<Interpreter.Token> line, int lineIndent, StringBuilder code) {
        code.append("if (")
                .append(parseCondition(line))
                .append(") {\n");
        blockIndents.push(lineIndent);
        blockTypes.push("if");
    }

    private void openForBlock(List<Interpreter.Token> line, int lineIndent, StringBuilder code) {
        // parse 'for var in range(...)'
        code.append(parseForLoop(line));
        blockIndents.push(lineIndent);
        blockTypes.push("for");
    }

    private void openWhileBlock(List<Interpreter.Token> line, int lineIndent, StringBuilder code) {
        code.append(parseWhile(line));
        blockIndents.push(lineIndent);
        blockTypes.push("while");
    }

    // --------------------- Parsing if, for, while --------------------- //

    private String parseCondition(List<Interpreter.Token> line) {
        // skip 'if' / 'elif' => parse until ':'
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < line.size(); i++) {
            if (":".equals(line.get(i).value)) break;
            sb.append(line.get(i).value);
        }
        return fixExpr(sb.toString().trim());
    }

    private String parseForLoop(List<Interpreter.Token> line) {
        // e.g. for number in range(1, M):
        // line[1] => varName
        String varName = line.get(1).value;
        if (!varTypes.containsKey(varName)) {
            varTypes.put(varName, "int");
        }

        int openP = -1, closeP = -1;
        for (int i = 0; i < line.size(); i++) {
            if ("(".equals(line.get(i).value)) {
                openP = i;
                break;
            }
        }
        for (int i = openP+1; i < line.size(); i++) {
            if (")".equals(line.get(i).value)) {
                closeP = i;
                break;
            }
        }
        StringBuilder inside = new StringBuilder();
        for (int i = openP+1; i < closeP; i++) {
            inside.append(line.get(i).value);
        }
        String rangeText = inside.toString().trim();
        String[] parts = splitTopComma(rangeText);

        String startExpr = "0";
        String endExpr   = null;
        String stepExpr  = "1";
        if (parts.length == 1) {
            endExpr = parts[0];
        } else if (parts.length == 2) {
            startExpr = parts[0];
            endExpr   = parts[1];
        } else if (parts.length == 3) {
            startExpr = parts[0];
            endExpr   = parts[1];
            stepExpr  = parts[2];
        }
        startExpr = fixExpr(startExpr);
        endExpr   = fixExpr(endExpr != null ? endExpr : "0");
        stepExpr  = fixExpr(stepExpr);

        StringBuilder sb = new StringBuilder();
        // declare variable if not declared
        if (varTypes.containsKey(varName)) {
            varTypes.put(varName, "int");
            sb.append("int ").append(varName).append(";\n");
        } else if (!"int".equals(varTypes.get(varName))) {
            // fallback to int if unknown or conflicting
            varTypes.put(varName, "int");
            sb.append("int ").append(varName).append(";\n");
        }

        sb.append(varName).append(" = ").append(startExpr).append(";\n");

        sb.append("for (; ")
                .append(varName).append(" < ").append(endExpr)
                .append("; ");

        if ("1".equals(stepExpr)) {
            sb.append(varName).append("++");
        } else {
            sb.append(varName).append(" += ").append(stepExpr);
        }
        sb.append(") {\n");

        return sb.toString();
    }

    private String parseWhile(List<Interpreter.Token> line) {
        // "while b:" => while (b != 0) {
        // "while b < 10:" => while (b<10) {
        if (line.size() >= 3 && ":".equals(line.get(2).value)) {
            String var = line.get(1).value;
            if (!varTypes.containsKey(var)) {
                varTypes.put(var, "int");
            }
            return "while (" + var + " != 0) {\n";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < line.size(); i++) {
                if (":".equals(line.get(i).value)) break;
                sb.append(line.get(i).value);
            }
            return "while (" + fixExpr(sb.toString().trim()) + ") {\n";
        }
    }

    // --------------------- Normal statements --------------------- //

    private String translateLine(List<Interpreter.Token> line) {
        StringBuilder sb = new StringBuilder();
        for (Interpreter.Token t : line) {
            sb.append(t.value);
        }
        String raw = sb.toString().trim();

        // convert print(...)
        if (raw.startsWith("print(") && raw.endsWith(")")) {
            String inside = raw.substring("print(".length(), raw.length()-1);
            return "System.out.println(" + convertPrint(inside) + ");";
        }

        // assignment => var = expr
        if (raw.contains("=") && !raw.contains("==")) {
            String[] parts = raw.split("=");
            if (parts.length == 2) {
                String lhs = parts[0].trim();
                String rhs = fixExpr(parts[1].trim());
                String t = guessType(rhs);
                if (!varTypes.containsKey(lhs)) {
                    varTypes.put(lhs, t);
                    return t + " " + lhs + " = " + rhs + ";";
                } else {
                    return lhs + " = " + rhs + ";";
                }
            }
        }

        // break/continue
        if ("break".equals(raw) || "continue".equals(raw)) {
            return raw + ";";
        }

        // add semicolon if none
        if (!raw.endsWith("{") && !raw.endsWith("}") && !raw.endsWith(";")) {
            raw += ";";
        }
        return raw;
    }

    // --------------------- Utility: print, expr, type, etc. --------------------- //

    private String convertPrint(String inside) {
        String[] arr = inside.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i>0) sb.append(" + ");
            sb.append(fixExpr(arr[i].trim()));
        }
        return sb.toString();
    }

    private String fixExpr(String expr) {
        if (expr == null) return "0";
        expr = expr.replace("True", "true").replace("False", "false");
        expr = expr.replaceAll("\\bint\\((.*?)\\)", "(int)($1)");
        return expr.trim();
    }

    private String guessType(String x) {
        if ("true".equals(x) || "false".equals(x)) return "boolean";
        if (x.matches("\\d+")) return "int";
        if (x.matches("\\d+\\.\\d+")) return "double";
        if (varTypes.containsKey(x)) return varTypes.get(x);
        return "int";
    }

    /**
     * Splits a string by top-level commas (i.e., commas not inside parentheses).
     * For range(2, (x+3)), we do a small parse so that the parentheses are considered.
     */
    private String[] splitTopComma(String text) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
                cur.append(c);
            } else if (c == ')') {
                depth--;
                cur.append(c);
            } else if (c == ',' && depth==0) {
                list.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            list.add(cur.toString());
        }
        return list.toArray(new String[0]);
    }
}
