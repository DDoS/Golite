package test

var a int
//Blank id should work but assigning a to a float should fail
func xyz() {
	_, a = a, 1.0
}