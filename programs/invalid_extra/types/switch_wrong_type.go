// Wrong type used in case conditional
package main

func main() {
    switch i:= 1; i {
        case 1:
            println("One")
        case 2.0: // Should be an int
            println(i)
    }
}