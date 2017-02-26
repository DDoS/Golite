////////////////////////////////////////////////////////////////////////////
//////////////////////////* MergeSort in GoLite *//////////////////////////
///////////////////////////////////////////////////////////////////////////
package main

var intArr[10] int

func sort(low, high int)[10] int {

    if (low < high) {
        var middle int
        middle = (low + high) / 2

        sort(low, middle)
        sort(middle + 1, high)
        merge(low, middle, high)
    }
    return intArr;

}

func merge(low, middle, high int) {

    var arr[10] int
    var i, j, k int

    for i = low;
    i <= middle;
    i++{
        arr[i] = intArr[i]
    }

    for j = middle + 1; j <= high; j++{
        arr[high + middle + 1 - j] = intArr[j]
    }

    i = low
    j = high


    for k = low; k <= high; k++{
        if (arr[i] <= arr[j]) {
            intArr[k] = arr[i]
            i++
        } else {
            intArr[k] = arr[j]
            j--
        }

    }
}

func main() {

    var i int
    var arr[10] int

    // Initialize the array
    intArr[0] = 2
    intArr[1] = 31
    intArr[2] = 61
    intArr[3] = 18
    intArr[4] = 10
    intArr[5] = 21
    intArr[6] = 14
    intArr[7] = 161
    intArr[8] = 18
    intArr[9] = 21


    // Unsorted Array
    print("Unsorted Array : ")

    for i = 0; i < 10; i++{
        print(intArr[i])
        print(" ")
    }

    arr = sort(0, 9)

    // Sorted Array
    print("\n Sorted Array  : ")

    for i = 0; i < 10; i++{
        print(arr[i])
        print(" ")
    }
}
