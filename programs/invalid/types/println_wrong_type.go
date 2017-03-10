// Invalid type in println statement
package main

type strt struct {
	i int
	f float64
	s string
}

func main() {
	var vr strt
	println(vr) // Can't print a struct
}