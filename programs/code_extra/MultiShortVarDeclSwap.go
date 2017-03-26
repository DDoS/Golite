package main

func main() {
    var a = 1
    var b = 2

    x, b, y, a, z := 10, a, 11, b, 12

    if a != 2 || b != 1 {
        println("Expected a = 2 && b = 1, but got a = ", a, " && b = ", b)
    } else {
        println("All good :)")
    }
}
