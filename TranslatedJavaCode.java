public class TranslatedJavaCode {
    public static void main(String[] args) {
int N = 10;
int M = 11;
int sum = 0;
int number;
number = 1;
for (; number < M; number++) {
sum = sum+number;
}
System.out.println(" Sum of first " + N + " numbers: " + sum);
int A = 5;
int AA = 6;
int factorial = 1;
int i;
i = 1;
for (; i < AA; i++) {
factorial = factorial*i;
}
System.out.println(" Factorial of " + A + " : " + factorial);
int a = 48;
int b = 18;
while (b != 0) {
int tempA = a;
a = b;
b = tempA%b;
}
int gcd = a;
System.out.println("GCD of 48 and 18:" + gcd);
number = 1234;
int reversed_number = 0;
while (number!=0) {
int digit = number%10;
reversed_number = reversed_number*10+digit;
number = (number-digit)/10;
}
System.out.println("Reversed number of 1234:" + (int)(reversed_number));
N = 13;
boolean is_prime = true;
if (N<3) {
is_prime = false;
}
else if (N%2==0) {
is_prime = false;
}
else {
i = 2;
while (i<(int)(N/2)) {
if (N%i==0) {
is_prime = false;
break;
}
i = i+1;
}
}
System.out.println("Is " + N + " a prime number: " + is_prime);
    }
}
