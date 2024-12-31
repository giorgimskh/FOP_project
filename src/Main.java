import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.FileWriter;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;


public class Main {
    public static void main(String[] args) {
        // Path to the Python script to be converted.
        // Tester of this program should change path with their own,
        // because this path is for my computer and may not work on different machine.
        // It is desired to copy txt.file which is uploaded on GitHub.
        String filePath = "C:\\Users\\user\\Desktop\\FOP\\python_code_test.txt";

        // 1) interpreter parsing text and adding at the List(lines)
        List<List<Interpreter.Token>> tokens = Interpreter.parse(filePath);

        // 2) Convert the list into java code
        PythonToJavaConverter converter = new PythonToJavaConverter(tokens);
        String javaCode = converter.convert();

        // For better readability I print translated java code on console.
        System.out.println(javaCode);

        // 3) We generate class where java code will be saved and then run.
        String className = "TranslatedJavaCode";
        saveJavaCode(className, javaCode);

        // 4) Compile the .java file and then execute its main method.
        compileAndRunJava(className);
    }

    //this creates the java file with its class and main where given python code translated to java
    //is places inside the main
    private static void saveJavaCode(String className, String javaCode) {
        try {
            //naming the file
            String fileName = className + ".java";
            FileWriter writer = new FileWriter(fileName);

            //Writing all neccessary things to run the main
            //Firstly writing the public class
            writer.write("public class " + className + " {\n");
            writer.write("    public static void main(String[] args) {\n");
            writer.write(javaCode);
            writer.write("    }\n");
            writer.write("}\n");

            writer.close();

            //closing the writer in order to not make the programm slow
        } catch (Exception e) {
            System.err.println("Error writing Java file: " + e.getMessage());
        }
    }


     // Compiles the generated Java file, then loads and invokes the main() method

    private static void compileAndRunJava(String className) {
        // Obtain the system Java compiler (part of the JDK).
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("No Java compiler found. Make sure you're running with a JDK, not just a JRE.");
            return;
        }

        // Compile the .java file. If result == 0, compilation succeeded.
        int result = compiler.run(null, null, null, className + ".java");
        if (result == 0) {
            try {
                // Use the current directory as the 'root' for class loading.
                File root = new File("");
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});

                // Load the compiled class by name.
                Class<?> cls = Class.forName(className, true, classLoader);

                // Find the class's main() method and invoke it with empty args.
                Method main = cls.getDeclaredMethod("main", String[].class);
                main.invoke(null, (Object) new String[]{});
            } catch (Exception e) {
                System.err.println("Error running compiled class: " + e.getMessage());
            }
        } else {
            System.err.println("Compilation failed.");
        }
    }
}