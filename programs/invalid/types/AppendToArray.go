package main

func main() {
    var array [16]int
    // Can only append to an array, not a slice
    array = append(array, 2)
}
