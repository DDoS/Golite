package test

func hi(x, y int) {
	//Only one expr allowed
	y, x &^= x, y
}