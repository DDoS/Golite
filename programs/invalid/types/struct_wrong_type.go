// Accessing var in struct with wrong type
package main

type strt struct {
	i int
	f float64
	s string
}

func main() {
	var vr strt
	vr.i = 10.1 // This is declared as an int
}