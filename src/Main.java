import java.util.*;



public class Main {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\user\\Desktop\\FOP\\python_code_test.txt"; // Ensure this path points to your file
        List<List<Interpreter.Token>> tokens = Interpreter.parse(filePath);


        for (List<Interpreter.Token> tokenList : tokens) {
            System.out.println(tokenListToString(tokenList));
        }

        Execution.eval(tokens);
    }

    private static String tokenListToString(List<Interpreter.Token> tokenList) {
        StringBuilder result = new StringBuilder();
        for (Interpreter.Token token : tokenList) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(token.toString());
        }
        return result.toString();
    }
}


