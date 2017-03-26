package main

//Outputs the first 100,000 digits of pi

var scale int = 10000
var arrinit int = 2000

func pi_digs() {
        //change digits and the size of array to the number of pi digits to be generated (and that number + 1 respectively)
        var digits int = 100000
        var arr [100001]int
        var carry int = 0
        var sum int = 0

        for i := 0; i < digits; i++ {
                arr[i] = arrinit
        }

        for i:=digits; i > 0; {
                sum = 0
                for j:= i; j > 0; j-- {
                        sum = (sum * j) + (scale * arr[j])
                        arr[j] = sum % (j*2 - 1)
                        sum = sum / ((j*2) - 1)
                }
                print(carry + sum/scale)
                carry = sum % scale

                i = i - 14
        }

}

func main() {
        pi_digs()
}

