import java.util.List;

class Handling {

    static void AssignmentHandling(List<Interpreter.Token> line) {
        String varName = line.getFirst().value;

        for (int i = 2; i < line.size(); i++) {
            if (line.get(i).type.equals("Number")) {
                int value = Integer.parseInt(line.get(i).value);
                int previousValue = Execution.Variables.get(varName) == null ? 0 : Execution.Variables.get(varName);
                //above i want to add previous value to current value(res=10+10)

                Execution.Variables.put(varName, previousValue + value);
                continue;
            }

            if(line.get(i).type.equals("String")) {
                String quotedString = line.get(i).value;
                String trimmedString = quotedString.substring(1, quotedString.length() - 1);

                Execution.VariablesForString.put(varName,trimmedString);
                break;
            }
        }
    }

    static void PrintHandling(List<Interpreter.Token> line){
        if (line.get(0).type.equals("String")) {
            String quotedString = line.get(2).value;

            if (quotedString != null && quotedString.startsWith("\"") && quotedString.endsWith("\"") && quotedString.length() > 1) {
                String trimmedString = quotedString.substring(1, quotedString.length() - 1);
                System.out.println(trimmedString);
            } else {
                System.out.println(Execution.VariablesForString.get(quotedString));
            }
        }
    }

}

