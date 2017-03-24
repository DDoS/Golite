// Typecast from int->float->int->float

package main

var intvar int = 10
var floatvar float64 = float64(intvar)
var intvars int = int(floatvar)
var floatvars float64 = float64(intvars)

func main() {
    print ("Final value of number is", floatvars)
}
