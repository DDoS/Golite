//Prints a 'staircase' of "#" in the format outlined at: 
//https://www.hackerrank.com/challenges/staircase"
package main

func main() {
	staircase(5)
}
func staircase(n int) {
	for i := 1; i <= n; i++ {
		num_spaces := n-i
		num_hash := i
		for k:= 1; k<=num_spaces; k++ {
			print(" ")
		}
		for j:=1; j<=num_hash; j++ {
			print("#");
		}
		println();
	}
}