package main

// Two fields can't have the same name in a struct
type Stuff struct {
    a int
    b bool
    a struct {
        r rune
    }
}

func main() {

}
