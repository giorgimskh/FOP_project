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
        String filePath = "C:\\Users\\user\\Desktop\\FOP\\python_code_test.txt";
        List<List<Interpreter.Token>> tokens = Interpreter.parse(filePath);

        PythonToJavaConverter converter = new PythonToJavaConverter(tokens);
        String javaCode = converter.convert();

        // Print the converted Java code (Optional)
        System.out.println(javaCode);

        // Save the generated Java code to a file
        String className = "TranslatedJavaCode"; // Name of the Java class to be created
        saveJavaCode(className, javaCode);

        // Compile and execute the Java file
        compileAndRunJava(className);
    }

    private static void saveJavaCode(String className, String javaCode) {
        try {
            String fileName = className + ".java";
            FileWriter writer = new FileWriter(fileName);
            writer.write("public class " + className + " {\n");
            writer.write("    public static void main(String[] args) {\n");
            writer.write(javaCode);
            writer.write("    }\n");  // End of main method
            writer.write("}\n");      // End of class
            writer.close();
        } catch (Exception e) {
            System.err.println("Error writing Java file: " + e.getMessage());
        }
    }


    private static void compileAndRunJava(String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("No Java compiler available. Make sure you are running this with a JDK and not just a JRE.");
            return;
        }

        int result = compiler.run(null, null, null, className + ".java");
        if (result == 0) {
            try {
                File root = new File(""); // current directory
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
                Class<?> cls = Class.forName(className, true, classLoader);
                Method main = cls.getDeclaredMethod("main", String[].class);
                String[] args = null; // main method args, if needed
                main.invoke(null, (Object) args);
            } catch (Exception e) {
                System.err.println("Error running class " + className + ": " + e.getMessage());
            }
        } else {
            System.err.println("Compilation failed.");
        }
    }
}
