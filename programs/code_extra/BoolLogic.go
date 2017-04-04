package main

func falseBool() bool {
    return false
}

func trueBool() bool {
    return true
}

func main() {
    println(trueBool() && trueBool() || falseBool() && falseBool())
}
