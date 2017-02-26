////////////////////////////////////////////////////////
/* Golite program to check if a number is a palidrome */
////////////////////////////////////////////////////////
package main

func findPalidrome() {

    // Variables

    var rem, sum int
    var temp, num int
    rem, sum = 0, 0

    //Number to be checked for palindrome  

    num = 56788765

    temp = num

    // Run the actual loop

    for num > 0 {
        rem = num % 10 //Remainder  
        sum = (sum * 10) + rem
        num = num / 10
    }

    //Print the messages

    if temp == sum {
        print("num is a palidrome")
    } else {
        print("num is NOT a palidrome")
    }
}


// Call the findPalidrome function

func main() {
    // Call the findPalidrome function
    findPalidrome()
}
