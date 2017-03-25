package main

func main() {
    var a = 1
    var b = 2

    c, b, a := 1, a, b

    if a != 2 || b != 1 {
        println("Expected a = 2 && b = 1, but got a = ", a, " && b = ", b)
    } else {
        println("All good :)")
    }
}
