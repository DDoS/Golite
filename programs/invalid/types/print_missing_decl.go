// Undeclared variable in print statement
package main

type strt struct {
    i int
    f float64
}

func main() {
    var vr strt
    print(vr.s) // vr is not declared
}