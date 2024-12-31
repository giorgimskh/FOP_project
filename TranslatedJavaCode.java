public class TranslatedJavaCode {
    public static void main(String[] args) {
int N = 13;
boolean is_prime = true;
if (N<3) {
is_prime = false;
} else if (N%2==0) {
is_prime = false;
} else {
i = 2;
for (; i < N/2; i++) {
if (N%i==0) {
is_prime = false;
break;
}
System.out.println("Is" + N + "a prime number:" + is_prime);
    }
}
