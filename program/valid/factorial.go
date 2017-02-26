//Given int n, returns the factorial of n
//n! = n*(n-1)*...*1
package main

func fact(n int) int {
	fact := n
	for n := n-1; n>1; n-- {
		fact = fact * n
	}
	return fact
}

func main() {
	print(fact(5))
}