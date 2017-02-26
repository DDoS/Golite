package main

func recursivecall() {
   recursivecall() 
}

func main() {

//var a int

for iter := 0; iter < 10; iter++ {
		recursivecall()
	}
   
}