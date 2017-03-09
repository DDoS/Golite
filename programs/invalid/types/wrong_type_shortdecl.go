package test

var a int
var b float64
var c int

func x() {
    //Wrong type assignments for short declaration
    var a, b float64
	a, c, b := 1.0, 2, 3.0
}