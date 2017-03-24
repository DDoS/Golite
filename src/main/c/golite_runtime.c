#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>

typedef struct {
    int32_t length;
    int8_t* data;
} goliteRtSlice;

void goliteRtPrintBool(int8_t c) {
    printf("%s", c ? "true" : "false");
}

void goliteRtPrintInt(int32_t i) {
    printf("%i", i);
}

void goliteRtPrintRune(int32_t rune) {
    int32_t str[2] = {rune, 0};
    printf("%ls", str);
}

void goliteRtPrintFloat64(double d) {
    printf("%f", d);
}

void goliteRtPrintString(goliteRtSlice str) {
    for (int32_t i = 0; i < str.length; i++) {
        putchar(str.data[i]);
    }
}

void goliteRtCheckBounds(int32_t index, int32_t length) {
    if (index < 0 || index >= length) {
        printf("Index %i out of bounds [%i, %i)\n", index, 0, length);
        exit(1);
    }
}

void staticInit();

#pragma GCC diagnostic ignored "-Wmain-return-type"
void main();

void start() {
    staticInit();
    main();
    exit(0);
}
