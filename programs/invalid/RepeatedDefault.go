package main

func main() {
    a := 2
    switch a {
    case 0:
        println("what")
    case 1:
        println("is")
    default:
        println("this")
    case 3:
        println("used")
    default:
        // Oops, a second default case
        println("for")
    }
}
