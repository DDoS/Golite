package test

func yo() {
	switch {
		//Empty case expr is not allowed
		case : print("bye")
	}
}