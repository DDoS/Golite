package main

type boolean bool

var k = 0

func _() {
}

/*func abs(i int) int {
    if i < 0 {
        return -i
    }
    return i
}*/

func main() {
    println(1, "lol\\", 0x10, `No\r escape`, false)
    var k = "test"
    //println(k)
    {
        var k boolean
        //println(1.3, k)
    }
    {
        //var k int = abs(3)
        //println(2, k)
    }
    //(abs)(4)
    (boolean)(true)
    var _ = 1
}
