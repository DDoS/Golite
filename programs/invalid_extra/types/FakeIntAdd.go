package main

type int bool

func main() {
    var a int
    var b int
    // Int was redefined as bool, and we can't add bools
    c := a + b
}
