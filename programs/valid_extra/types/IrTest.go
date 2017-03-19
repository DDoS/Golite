package main

var i = 0

func main() {
    println(1, "lol\\", 0x10, `No\r escape`)
    var k = "test"
    println(k)
    {
        var k bool
        println(k)
    }
}
