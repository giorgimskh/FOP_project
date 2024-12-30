import java.util.List;

public class PythonToJavaConverter {
    private List<List<Interpreter.Token>> tokens;

    public PythonToJavaConverter(List<List<Interpreter.Token>> tokens) {
        this.tokens = tokens;
    }

    public String convert() {
        StringBuilder javaCode = new StringBuilder();
        for (List<Interpreter.Token> lineTokens : tokens) {
            if (!lineTokens.isEmpty() && "for".equals(lineTokens.get(0).value)) {
                javaCode.append(parseForLoop(lineTokens)).append("}\n");  // Closing brace added for the loop
            } else if ("symbol".equals(lineTokens.get(0).type) && "=".equals(lineTokens.get(1).value)) {
                String assignmentCode = parseAssignment(lineTokens);
                if (!isRedundantAssignment(lineTokens)) {
                    javaCode.append(assignmentCode).append("\n");
                }
            } else if ("print".equals(lineTokens.get(0).value)) {
                javaCode.append(parsePrint(lineTokens)).append("\n");
            }
        }
        return javaCode.toString();
    }

    private boolean isRedundantAssignment(List<Interpreter.Token> tokens) {
        if (tokens.size() > 2 && tokens.get(0).value.equals(tokens.get(2).value)) {
            return true; // Checks if the variable is assigned to itself, e.g., x = x;
        }
        return false;
    }

    private String parseForLoop(List<Interpreter.Token> tokens) {
        String var = tokens.get(1).value;
        int start = Integer.parseInt(tokens.get(5).value);
        String end = tokens.get(7).value;
        return String.format("for (int %s = %d; %s < %s; %s++) {\n    sum = sum + %s;\n", var, start, var, end, var, var);
    }

    private String parseAssignment(List<Interpreter.Token> tokens) {
        String var = tokens.get(0).value;
        String value = tokens.get(2).value;
        if (tokens.get(2).type.equals("number")) {
            return String.format("int %s = %s;", var, value);
        } else {
            return String.format("%s = %s;", var, value);
        }
    }

    private String parsePrint(List<Interpreter.Token> tokens) {
        StringBuilder output = new StringBuilder("System.out.println(");
        for (int i = 1; i < tokens.size(); i++) {
            Interpreter.Token token = tokens.get(i);
            if ("string".equals(token.type)) {
                output.append(token.value);
            } else if ("symbol".equals(token.type)) {
                output.append(" + ").append(token.value).append(" + ");
            }
        }
        output.append(");");
        return output.toString().replace(" + )", ")");
    }
}
