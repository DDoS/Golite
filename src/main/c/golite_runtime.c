#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

typedef struct {
    int32_t length;
    int8_t* data;
} goliteRtSlice;

const int32_t EQ = 0;
const int32_t NEQ = 1;
const int32_t LESS = 2;
const int32_t LESS_EQ = 3;
const int32_t GREAT = 4;
const int32_t GREAT_EQ = 5;

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

int8_t goliteRtCompareString(int32_t kind, goliteRtSlice str1, goliteRtSlice str2) {
    int minLength = str1.length < str2.length ? str1.length : str2.length;
    int cmp = memcmp(str1.data, str2.data, minLength * sizeof(int8_t));
    switch (kind) {
        case EQ:
            return cmp == 0 && str1.length == str2.length;
        case NEQ:
            return cmp != 0 || str1.length != str2.length;
        case LESS:
            return cmp < 0 || (cmp == 0 && str1.length < str2.length);
        case LESS_EQ:
            return cmp < 0 || (cmp == 0 && str1.length <= str2.length);
        case GREAT:
            return cmp > 0 || (cmp == 0 && str1.length > str2.length);
        case GREAT_EQ:
            return cmp > 0 || (cmp == 0 && str1.length >= str2.length);
    }
    printf("Bad comparison kind: %i\n", kind);
    exit(1);
}

void staticInit();
void goliteMain();

int main(int argc, char** argv) {
    staticInit();
    goliteMain();
    return 0;
}
