// MergeSort a huge array in GoLite

package main

var intArr [1000000]int

func merge(low, middle, high int) {

	var arr [1000000]int
	var i, j, k int

	for i = low; i <= middle; i++ {
		arr[i] = intArr[i]
	}

	for j = middle + 1; j <= high; j++ {
		arr[high+middle+1-j] = intArr[j]
	}

	i = low
	j = high

	for k = low; k <= high; k++ {
		if arr[i] <= arr[j] {
			intArr[k] = arr[i]
			i++
		} else {
			intArr[k] = arr[j]
			j--
		}

	}
}

func sort(low, high int) [1000000]int {

	if low < high {
		var middle int
		middle = (low + high) / 2

		sort(low, middle)
		sort(middle+1, high)
		merge(low, middle, high)
	}
	return intArr

}

// Generate random numbers
var initial int = 914
var newInt int = initial

func rand(i int) int {
	newInt = ((9*i)+7)%5 + newInt
	return newInt
}

func main() {
	var size int = 999999
	var random int
	var arr [1000000]int

	// Populate the array with random numbers
	for i := 0; i < size; i++ {
		random = rand(i)
		intArr[i] = random
	}

	print("List generated of size, size+1")

	// Unsorted Array
	print("Unsorted Array : ")

	for i := 0; i < 1000000; i++ {
		print(intArr[i])
		print(" ")
	}

	arr = sort(0, 999999)

	// Sorted Array
	print("\n Sorted Array  : ")

	for i := 0; i < 1000000; i++ {
		print(arr[i])
		print(" ")
	}
}