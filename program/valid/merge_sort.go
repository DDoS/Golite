////////////////////////////////////////////////////////////////////////////
//////////////////////////* MergeSort in GoLite *//////////////////////////
///////////////////////////////////////////////////////////////////////////

package main

var numbers [10]int
var helper [10]int
var number int

func sort(values [10]int) {
    numbers = values
    number = 10 //values.length

    Mergesort(0, number - 1)
}


// If low is not smaller then high then the array is sorted
func Mergesort(low, high int) {
    var middle int
    if low < high {
        middle = low + (high - low) / 2
        Mergesort(low, middle)
        Mergesort(middle + 1, high)
        merge(low, middle, high)
    }
}


func merge(low, middle, high int) {

   var it int

   // Copy into helper array
    for it = low; it <= high; it++ {
        helper[it] = numbers[it]
    }

    var i int = low
    var j int = middle + 1
    var k int = low

    for i = low; i <= middle; i++ {
        if j <= high {
            if helper[i] <= helper[j] {
                numbers[k] = helper[i]
                i++
            } else {
                numbers[k] = helper[j]
                j++
            }
            k++
        }
    }

// Copy rest of left array in target array
    for i = i; i <= middle; i++ {
        numbers[k] = helper[i]
        k++
        i++
    }
}

func main() {

	s := [10]int{2, 3, 5, 7, 9, 10, 12, 14, 16, 19}
	sort(s)
	
	// Print the sorted array
	print ("Sorted array is: ")
	var iter int
	for iter = 0 ; iter < 9 ; iter++ {
	print (s[iter] )
	print (" ")
	}
}


