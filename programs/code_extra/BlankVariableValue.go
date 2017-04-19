package main

var wasCalled1 = false

func needsToBeCalled1() int {
    wasCalled1 = true
    println("Called!")
    return 0
}

var wasCalled2 = false

func needsToBeCalled2() int {
    wasCalled2 = true
    println("Called!")
    return 0
}

func main() {
    a, _ := 1, needsToBeCalled1()
    if !wasCalled1 {
        println("NOT called!")
    }
    var _ = needsToBeCalled2()
    if !wasCalled2 {
        println("NOT called!")
    }
}
