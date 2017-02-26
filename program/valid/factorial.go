//Given int n, returns the factorial of n
//n! = n*(n-1)*...*1
package main

func rec_factorial(n int) int {
	if (n == 1) {
		return n;
	} else {
		return n * rec_factorial(n-1)
	}
}

func main() {
	print(rec_factorial(5))
}
