package test

func noRet() {
}

type boolean bool
var b2 boolean = boolean(true)

func test(b bool, i int) float64 {
    var b = true
    for b  {
        var b = false
    }

    for b2 {
    }

    for i = 0; ; i++ {

    }
}

type Data []int
type Person struct {
    name string
    age int
}

var a Data
var b Person
var c = test(true, 1)
var d = int('a')
var e = append(a, d)
var f = b.age
var g = e[0]
var h = rune(41.32)
