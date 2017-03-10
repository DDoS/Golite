package main

type Stuff struct {
    field0 int
    field1 float64
}

func main() {
    var stuff Stuff
    // The struct field "field2" doesn't exist
    println(stuff.field0, stuff.field1, stuff.field2)
}
