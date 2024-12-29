import java.util.List;

class Handling {
    static void AssignmentHandling(List<Interpreter.Token> line) {
        String varName = line.getFirst().value;

        for (int i = 2; i < line.size(); i++) {
            if (line.get(i).type == "Number") {
                int value = Integer.parseInt(line.get(i).value);
                int previousValue = Execution.Variables.get(varName) == null ? 0 : Execution.Variables.get(varName);
                //above i want to add previous value to current value(res=10+10)

                Execution.Variables.put(varName, previousValue + value);
            }
        }
    }
}

