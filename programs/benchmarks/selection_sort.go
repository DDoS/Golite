// Selection Sorting a huge array in GoLite 

package main

var  SIZE int = 750
var intArr [750]int

 func selectionSort(arr [750]int){
 var low int
 var index int
        for i := 0; i < SIZE - 1; i++  {  
            index = i 
            for iIncr := i + 1; iIncr < SIZE; iIncr++ {  
                if arr[iIncr] < arr[index] {  
                    index = iIncr
                }  
            }  
            low = arr[index]
            arr[index] = arr[i]
            arr[i] = low  
        }  
    }  


// Generate random numbers
var initial int = 914
var newInt int = initial

func rand(i int) int {
	newInt = ((9*i)+7)%5 + newInt
	return newInt
}

func main() {
	var random int

	// Populate the array with random numbers
	for i := 0; i < SIZE ; i++ {
		random = rand(i)
		intArr[i] = random
	}

	print("List generated of size ", SIZE )

	// Unsorted Array
	print("\nBeginning Sort..... \n")

	selectionSort(intArr)

	// Sorted Array
	print("Array Sorting Finished.\n")

}