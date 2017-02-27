package main

func main() {
    var a int
    for i := 1; i < 17; i++ {
        a ^= i
    }
    // This is can't be outside the loop
    break
}
