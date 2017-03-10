//Op-Statement invalid types
package main

func main() {
    var i int = 1
    var f float64 = 1.0
    
    i %= i // Valid
    i %= f // Invalid
}