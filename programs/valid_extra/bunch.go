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

    for i = 0; i < 10; i++ {

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
var i = !b2
var j = ^'1'
var k = +3.4
var l = -2
var m = "l" + "o" + "l"
var n Person
var o = b != n

var a1 = 1 * 2
var a2 = 1 / 2
var a3 = 1 - 2
var a4 = 1 + 2
var a5 = 1 % 2
var a6 = 1 << 2
var a7 = 1 >> 2
var a8 = 1 & 2
var a9 = 1 &^ 2
var a10 = 1 | 2
var a11 = 1 ^ 2
var a12 = 1 == 2
var a13 = 1 != 2
var a14 = 1 < 2
var a15 = 1 <= 2
var a16 = 1 > 2
var a17 = 1 >= 2
var a18 = false && true
var a19 = false || true

func exprStmt() {
    test(false, -232)
    append(a, 5 * 9)
}
