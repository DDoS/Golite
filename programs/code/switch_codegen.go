package main

func main() {
    
    var intvar int = 2
    
    switch true || false && 5 == 2 {
    case intvar > 1:
        println("> 1")
    case intvar > 2:
        println("> 2")
    default:
        println("other")
    case intvar > 3:
        println("> 3")
    }
}
