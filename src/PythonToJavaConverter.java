import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PythonToJavaConverter {
    private final List<List<Interpreter.Token>> tokens;
    private final HashMap<String, String> varTypes = new HashMap<>();

    public PythonToJavaConverter(List<List<Interpreter.Token>> t) {
        tokens = t;
    }

    public String convert() {
        StringBuilder code = new StringBuilder();
        boolean inBlock = false;
        int currentIndent = -1;

        for (int i = 0; i < tokens.size(); i++) {
            List<Interpreter.Token> line = tokens.get(i);
            if (line.isEmpty()) {
                continue;
            }
            int lineIndent = line.get(0).indentLevel;
            String first = line.get(0).value;

            if ("if".equals(first)) {
                if (inBlock && lineIndent <= currentIndent) {
                    code.append("}\n");
                    inBlock = false;
                }
                inBlock = true;
                currentIndent = lineIndent;
                code.append("if (")
                        .append(parseCondition(line))
                        .append(") {\n");
            }
            else if ("elif".equals(first)) {
                code.append("} else if (")
                        .append(parseCondition(line))
                        .append(") {\n");
            }
            else if ("else".equals(first)) {
                code.append("} else {\n");
            }
            else if ("for".equals(first)) {
                if (inBlock && lineIndent <= currentIndent) {
                    code.append("}\n");
                    inBlock = false;
                }
                inBlock = true;
                currentIndent = lineIndent;
                code.append(parseForLoop(line));
            }
            else if ("while".equals(first)) {
                if (inBlock && lineIndent <= currentIndent) {
                    code.append("}\n");
                    inBlock = false;
                }
                inBlock = true;
                currentIndent = lineIndent;
                code.append(parseWhile(line));
            }
            else {
                if (inBlock && lineIndent <= currentIndent) {
                    code.append("}\n");
                    inBlock = false;
                }
                code.append(translateLine(line)).append("\n");
            }
        }

        if (inBlock) {
            code.append("}\n");
        }
        return code.toString();
    }

    private String parseCondition(List<Interpreter.Token> line) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < line.size(); i++) {
            if (":".equals(line.get(i).value)) {
                break;
            }
            sb.append(line.get(i).value);
        }
        return fixExpr(sb.toString().trim());
    }

    private String parseForLoop(List<Interpreter.Token> line) {
        String varName = line.get(1).value;
        if (!varTypes.containsKey(varName)) {
            varTypes.put(varName, "int");
        }

        int openParen = -1, closeParen = -1;
        for (int i = 0; i < line.size(); i++) {
            if ("(".equals(line.get(i).value)) {
                openParen = i;
                break;
            }
        }
        for (int i = openParen + 1; i < line.size(); i++) {
            if (")".equals(line.get(i).value)) {
                closeParen = i;
                break;
            }
        }
        StringBuilder inside = new StringBuilder();
        for (int i = openParen + 1; i < closeParen; i++) {
            inside.append(line.get(i).value);
        }
        String content = inside.toString().trim();
        String[] parts = splitTopComma(content);

        String startExpr = "0";
        String endExpr = null;
        String stepExpr = "1";
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
        startExpr = fixExpr(startExpr);
        endExpr = fixExpr(endExpr != null ? endExpr : "0");
        stepExpr = fixExpr(stepExpr);

        StringBuilder sb = new StringBuilder();
        if (!varTypes.containsKey(varName)) {
            sb.append("int ").append(varName).append(";\n");
        }
        sb.append(varName).append(" = ").append(startExpr).append(";\n");

        if ("1".equals(stepExpr)) {
            sb.append("for (; ")
                    .append(varName).append(" < ").append(endExpr)
                    .append("; ")
                    .append(varName).append("++) {\n");
        } else {
            sb.append("for (; ")
                    .append(varName).append(" < ").append(endExpr)
                    .append("; ")
                    .append(varName).append(" += ")
                    .append(stepExpr).append(") {\n");
        }
        return sb.toString();
    }

    private String parseWhile(List<Interpreter.Token> line) {
        if (line.size() >= 3 && ":".equals(line.get(2).value)) {
            String v = line.get(1).value;
            if (!varTypes.containsKey(v)) {
                varTypes.put(v, "int");
            }
            return "while (" + v + " != 0) {\n";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < line.size(); i++) {
                if (":".equals(line.get(i).value)) {
                    break;
                }
                sb.append(line.get(i).value);
            }
            return "while (" + fixExpr(sb.toString().trim()) + ") {\n";
        }
    }

    private String translateLine(List<Interpreter.Token> line) {
        StringBuilder sb = new StringBuilder();
        for (Interpreter.Token t : line) {
            sb.append(t.value);
        }
        String raw = sb.toString().trim();
        if (raw.startsWith("print(") && raw.endsWith(")")) {
            String inside = raw.substring("print(".length(), raw.length() - 1);
            return "System.out.println(" + convertPrint(inside) + ");";
        }
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
        if ("break".equals(raw) || "continue".equals(raw)) {
            return raw + ";";
        }
        if (!raw.endsWith("{") && !raw.endsWith("}") && !raw.endsWith(";")) {
            raw += ";";
        }
        return raw;
    }

    private String convertPrint(String inside) {
        String[] arr = inside.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(" + ");
            sb.append(fixExpr(arr[i].trim()));
        }
        return sb.toString();
    }

    private String guessType(String x) {
        if ("true".equals(x) || "false".equals(x)) return "boolean";
        if (x.matches("\\d+")) return "int";
        if (x.matches("\\d+\\.\\d+")) return "double";
        if (x.contains("Math.pow")) return "double";
        if (varTypes.containsKey(x)) return varTypes.get(x);
        return "int";
    }

    private String fixExpr(String expr) {
        if (expr == null) return "0";
        expr = expr.replace("True", "true").replace("False", "false");
        expr = expr.replaceAll("\\bint\\((.*?)\\)", "(int)($1)");
        expr = handlePower(expr);
        return expr.trim();
    }

    private String handlePower(String e) {
        while (e.contains("**")) {
            int idx = e.indexOf("**");
            int left = idx - 1;
            while (left >= 0 && Character.isJavaIdentifierPart(e.charAt(left))) {
                left--;
            }
            String base = e.substring(left+1, idx).trim();
            int right = idx + 2;
            while (right < e.length() &&
                    (Character.isJavaIdentifierPart(e.charAt(right)) || e.charAt(right) == '.')) {
                right++;
            }
            String exponent = e.substring(idx+2, right).trim();
            String replacement = "Math.pow(" + base + "," + exponent + ")";
            e = e.substring(0, left+1) + replacement + e.substring(right);
        }
        return e;
    }

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
        if (cur.length() > 0) {
            list.add(cur.toString());
        }
        return list.toArray(new String[0]);
    }
}
