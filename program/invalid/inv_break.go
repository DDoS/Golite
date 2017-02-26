package test
 
func heyo() {
	for i := 0; i < 10; {
		if (i) {
			continue;
		}
		return;
	}
	//invalid break outside loop
	break;
}