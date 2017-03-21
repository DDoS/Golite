#include <stdio.h>

void __golite_runtime_printBool(char c) {
    printf("%s", c ? "true" : "false");
}

void __golite_runtime_printInt(int i) {
    printf("%i", i);
}

void __golite_runtime_printFloat64(double d) {
    printf("%f", d);
}

void __golite_runtime_printString(char* str) {
    printf("%s", str);
}
