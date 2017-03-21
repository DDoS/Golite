package main

var scale int = 10000
var arrinit int = 2000

func pi_digs() {
	//change digits and the size of array to the number of pi digits to be generated (and that number + 1 respectively)
	var digits int = 50000
	var arr [50001]int
	var carry int = 0
	var sum int = 0
	
	for i := 0; i < digits; i++ {
		arr[i] = arrinit
		//print(arr[i])
	}
	
	for i:=digits; i > 0; i = i - 14 {
		sum = 0
		for j:= i; j > 0; j-- {
			sum = (sum * j) + (scale * arr[j])
			arr[j] = sum % (j*2 - 1)
			sum = sum / ((j*2) - 1)
		}
		print(carry + sum/scale)
		carry = sum % scale
	}
	
}

func main() {
	pi_digs()
}