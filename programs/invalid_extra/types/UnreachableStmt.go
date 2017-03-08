package main

func main() {
    var v int
    for i := 0; i < 10; i++ {
        v = v + 2
        continue
        v = v / 2
    }
}
