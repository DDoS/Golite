#include <stdio.h>

void __golite_runtime_printInt(int i) {
    printf("%i", i);
}

void __golite_runtime_printString(char* str) {
    printf("%s", str);
}
