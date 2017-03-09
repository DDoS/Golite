package test

func noRet() {
    var i int
    {
        var i int
        {
            i = 0
            j := i
        }
    }
    var j int
    i, j, k := 1, 2, 4
}

type boolean bool
var b2 boolean = boolean(true)

func test1(b bool, i int) float64 {
    for b  {
        var b = false
        return 7.1
    }

    for b2 {
        var s1 = ""
        switch i {
        case 0:
            break
        case 1:
            var s2 = ""
        }
        break
    }

    for i := 0; i < 10; i++ {
        var r = 'l'
        continue
    }
    return 4.7
}

func test2(i int) string {
    if i := 0; true {
        for i := 0; i < 10; i++ {
            var r = 'l'
            continue
        }
        return "2"
	} else if i := 0; b2 {
        return "1"
	}
    return "3"
}

func test3(i int) string {
    if i := 0; true {
        return "3"
	} else if i := 0; b2 {
        return "2"
	} else {
        return "1"
	}
}

func test4() int {
    var a, b int
    a, b, c := 1, 2, 3
    return a
}

func test5(i int) string {
    switch i {
    case 0:
        return "3"
    case 1:
        return "2"
    }
    return "1"
}

func test6(i int) string {
    switch i {
    default:
        return "3"
    case 0:
        return "2"
    case 1:
        return "1"
    }
}

func infLoop() int {
    for {
        println("weeee");
    }
}

type Data []int
type Person struct {
    name string
    age int
}

var a Data
var b Person
var c = test1(true, 1)
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
