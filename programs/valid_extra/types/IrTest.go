package main

var k = 0

func main() {
    println(1, "lol\\", 0x10, `No\r escape`, false)
    var k = "test"
    //println(k)
    {
        var k bool
        //println(1.3, k)
    }
    {
        var k float64
        //println(2, k)
    }
}
