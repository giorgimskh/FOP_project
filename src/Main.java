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
        // Adjust the file path to your Python script
        String filePath = "C:\\Users\\user\\Desktop\\FOP\\python_code_test.txt";

        // 1) Parse the Python file into tokens
        List<List<Interpreter.Token>> tokens = Interpreter.parse(filePath);

        // 2) Convert tokens to Java code
        PythonToJavaConverter converter = new PythonToJavaConverter(tokens);
        String javaCode = converter.convert();

        // (Optional) Print the generated Java code
        System.out.println(javaCode);

        // 3) Save the generated Java code to a file
        String className = "TranslatedJavaCode";
        saveJavaCode(className, javaCode);

        // 4) Compile and run the Java code
        compileAndRunJava(className);
    }

    private static void saveJavaCode(String className, String javaCode) {
        try {
            String fileName = className + ".java";
            FileWriter writer = new FileWriter(fileName);
            // Write a top-level public class with a main() that holds the code
            writer.write("public class " + className + " {\n");
            writer.write("    public static void main(String[] args) {\n");
            writer.write(javaCode);
            writer.write("    }\n");
            writer.write("}\n");
            writer.close();
        } catch (Exception e) {
            System.err.println("Error writing Java file: " + e.getMessage());
        }
    }

    private static void compileAndRunJava(String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("No Java compiler found. Make sure you're running with a JDK, not just a JRE.");
            return;
        }
        int result = compiler.run(null, null, null, className + ".java");
        if (result == 0) {
            try {
                File root = new File(""); // current directory
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
                Class<?> cls = Class.forName(className, true, classLoader);
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
