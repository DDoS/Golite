package main

type boolean bool

var k = 0

func _() {
}

func abs(i int) int {
    if i < 0 {
        return -i
    }
    return i
}

type Person struct {
    name string
    age int
}

var person Person

func agePerson(person Person) Person {
    //person.age += 40
    //person.name = "Uncle " + person.name
    person.name = "Test"
    println(person.name, ' ', person.age)
    return person
}

func staticInit() {
    agePerson(person)
    var arr [16]string
    var sli []int
    arr[1] = "lol"
    sli = append(sli, 3)
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
    //var arr1 [16]Person
    //var arr2 [16]Person
    //println(arr1 != arr2)
    println(getBool1() && getBool2())
    println(getBool1() || getBool2())
}

func goliteMain() {
    testEquals()
}

func goliteMain1() {
    println("h" < "ha")
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
    {
        var k int = abs(3)
        println(float64(2), k)
    }
    println(abs(int(4.5)))
    j := boolean(true)
    var _ = 1
    staticInit()
    goliteMain()
    goliteMain1()
}
