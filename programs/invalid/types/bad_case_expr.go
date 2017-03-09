package test

type in int

func hey() {
	var a in
	var b = 1
	//Can't use type int for case expressions
	switch ; a {
		case b:
	}
}