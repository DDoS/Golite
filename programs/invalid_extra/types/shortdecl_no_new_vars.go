package test

var a, b int
//No new variables declared on LHS of short assignment
func x() {
    var a, b, c int
	a, b, c := 1, 2, a+a
}