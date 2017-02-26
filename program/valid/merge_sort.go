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

 var s [10]int
 
 s[0] = 2
 s[1] = 3
 s[2] = 6
 s[3] = 8
 s[4] = 10
 s[5] = 12
 s[6] = 14
 s[7] = 16
 s[8] = 18
 s[9] = 21

 sort(s)

}