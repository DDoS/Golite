package main

type boolean bool

var k = 0

func _() {
}

func goliteRtPrintInt(i int) {
    println(i)
}

func abs(i int) int {
    if i < 0 {
        return -i
    } else {
        return i
    }
}

func printAbs(i int) {
    if i < 0 {
        goliteRtPrintInt(-i)
    } else {
        goliteRtPrintInt(i)
    }
}

type Person struct {
    name string
    age int
}

var person Person

func agePerson(person Person) Person {
    //person.age += 40
    person.name = "Uncle " + person.name
    println(person.name, ' ', person.age)
    return person
}

func staticInit() {
    person.name = "Bob"
    agePerson(person)
    var arr [16]string
    var sli []int
    arr[1] = "lol"
    sli = append(sli, 36)
    sli[0] %= 8
    println(arr[1])
    println(sli[0])
}

func getBool1() bool {
    println("got bool " + "1")
    return false
}

func getBool2() bool {
    println("got " + "bool 2" + "")
    return false
}

func testEquals() {
    var arr1 [16]Person
    var arr2 [16]Person
    println(arr1 != arr2)
    println(getBool1() && getBool2())
    println(getBool1() || getBool2())
}

func goliteMain() {
    testEquals()
}

func goliteMain1() {
    println("h" < "ha")
}

var b2 bool

func test2(j int) string {
    if i := 0; j == 2 {
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

func test3(j int) string {
    if i := 0; j == 2 {
        return "3"
	} else if i := 0; b2 {
        return "2"
	} else {
        return "1"
	}
}

func test5(j int) string {
    switch i := 0; j {
    case 0:
        return "3"
    case 1:
        return "2"
    }
    return "1"
}

func test6(j int) string {
    switch i := 0; j {
    default:
        return "3"
    case 0:
        return "2"
    case 1:
        return "1"
    }
}

var str = "lol\\"

func main() {
    print(k, str, 0x10, `No\r escape`, false, '\n')
    var k string
    println(k)
    {
        var k boolean
        println(1.3, k)
    }
    if k := getBool2(); k {
        var k int = abs(3)
        println(float64(2), k)
    }
    printAbs(int(-4.5))
    j := boolean(true)
    var _ = 1
    staticInit()
    goliteMain()
    goliteMain1()
    println(test2(3))
    println(test3(2))
    println(test5(1))
    println(test6(0))
}
