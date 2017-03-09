package main

func main() int {
    i := 3

    if i == 1 {
        return 3
    } else if i == 2 {
        return 2
    } else if i == 3 {
        // There needs to be a return statement here
        i = 1
    } else {
        return -1
    }
}
