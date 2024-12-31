public class Func {
    public static String arrVal(String[] arr, int i) {
        try {
            return arr[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "null";
        }
    }
}
