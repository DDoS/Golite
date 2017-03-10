package main

type nat int

func test(i int) {
}

func main() {
    var j = nat(1)
    // Passing arguments doesn't do type resolution, and nat != int
    test(j)
}
