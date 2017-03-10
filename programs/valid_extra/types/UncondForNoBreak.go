package main

func test1() int {
    for {
        for {
            break
        }
        switch {
        default:
            break
        }
    }
}

func test2() int {
    switch {
    default:
        for {
            break
        }
        return 0
    case false:
        switch {
        default:
            break
        }
        return 1
    }
}
