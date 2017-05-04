/*
 * This file is part of GoLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 Aleksi Sapon, Rohit Verma, Ayesha Krishnamurthy <https://github.com/DDoS/Golite>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

#define EQ 0
#define NEQ 1
#define LESS 2
#define LESS_EQ 3
#define GREAT 4
#define GREAT_EQ 5

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
