import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Execution {
    static Map<String, Integer> Variables = new HashMap<>();

    public static void eval(List<List<Interpreter.Token>> list) {
        for (List<Interpreter.Token> line : list) {
                if(line.isEmpty()) continue;

                if(line.contains("=") && !line.contains("for") && !line.contains("while")){
                    Handling.AssignmentHandling(line);
                }
        }
    }
}
