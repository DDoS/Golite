#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

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

goliteRtSlice goliteRtSliceAppend(goliteRtSlice slice, int8_t* appendData, int32_t appendLength) {
    int32_t newLength = slice.length + appendLength;
    int8_t* newData = malloc(newLength * sizeof(int8_t));
    size_t oldEnd = slice.length * sizeof(int8_t);
    memcpy(newData, slice.data, oldEnd);
    memcpy(newData + oldEnd, appendData, appendLength * sizeof(int8_t));
    goliteRtSlice newSlice = {.length = newLength, .data = newData};
    return newSlice;
}

goliteRtSlice goliteRtSliceConcat(goliteRtSlice slice1, goliteRtSlice slice2) {
    return goliteRtSliceAppend(slice1, slice2.data, slice2.length);
}

void staticInit();

#pragma GCC diagnostic ignored "-Wmain-return-type"
void main();

void start() {
    staticInit();
    main();
    exit(0);
}
