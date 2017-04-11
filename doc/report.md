# Report

## Introduction

## Language and tools

## Syntax

### Lexing

### Parsing

### Weeding

## Semantics

### Types

### Symbols

### Contexts

### Type checking

### Other validation

Like validating the main?

## Code generation

### Target language

We have chosen to to targert low-level code: LLVM IR. More specifically,
we first convert Golite to a simpler custom IR before converting it to LLVM IR using the LLVM C API.

We used LLVM because as a backend, as it's used by several popular
languages like Rust, Swift, C, C++, D, etc. It can compile to many different targets and offers optimization passed for free.
LLVM IR is comparatively easy to work with, and due to its popularity is rather well documented
and standardized, meaning that if we get stuck somewhere we have documentation to help us.

It also has a proper API to generate it using objects, which is a lot nicer than trying to print a language like C.
With it we can manipulate the IR more freely. Since we're working with the AST directly, things like names don't matter.
If there's a conflict, LLVM will take care of generating new names when printing.

It has a major disadvantage: it is comparatively more difficult to implement than higher level languages
like C/C++, but we wanted to take on this challenging task and in the process learn more about LLVM.

Using LLVM allows us to get experience with using a powerful framework and toolchain, that powers a vast number of compilers
used in research and enterprise.

### First conversion to custom IR

After type-checking, we run the AST and `SemanticData` produced by our type-checker through our `IrConverter`.
The `IrConverter` visits AST nodes and converts them to our custom node types (found in `golite.ir.node`)
to facilitate conversion to LLVM IR. The `Program` class contains all the relevant information required
for code generation: the package name, global variables, and functions (through which enclosed statements
can be accessed). The conversion allows for complicated statements to be split up into multiple simple statements.

The IR also directly integrates the semantic data, meaning that we no longer have to carry it around in a separate
`SemanticData` class. It is thus better suited for the final compilation passes.

As an example of our custom IR, consider this simple Golite program:

```go
////////////////////////////////////////////////////////
/* Golite program to check if a number is a palidrome */
////////////////////////////////////////////////////////
package main

func findPalidrome() {

    // Variables
    var rem, sum int
    var temp, num int
    rem, sum = 0, 0

    // Number to be checked for palindrome  
    num = 56788765

    // Run the actual loop
    temp = num
    for num > 0 {
        rem = num % 10 //Remainder  
        sum = (sum * 10) + rem
        num = num / 10
    }

    // Print the messages
    if temp == sum {
        println("num is a palidrome")
    } else {
        println("num is NOT a palidrome")
    }
}

func main() {
    // Call the findPalidrome function
    findPalidrome()
}
```

It is converted into the following custom IR:

```go
package main

func findPalidrome() {
    var rem int
    assign(rem, 0)
    var sum int
    assign(sum, 0)
    var temp int
    assign(temp, 0)
    var num int
    assign(num, 0)
    var assignTmp int
    assign(assignTmp, 0)
    var assignTmp0 int
    assign(assignTmp0, 0)
    assign(rem, assignTmp)
    assign(sum, assignTmp0)
    assign(num, 56788765)
    assign(temp, num)
    jump(startFor)

startFor:
    jump(endFor, !(num > 0))
    assign(rem, (num % 10))
    assign(sum, ((sum * 10) + rem))
    assign(num, (num / 10))
    jump(continueFor)

continueFor:
    jump(startFor)

endFor:
    jump(ifCase, (temp == sum))
    jump(elseCase)

ifCase:
    printString("num is a palidrome")
    printString("\0A")
    jump(endIf)

elseCase:
    printString("num is NOT a palidrome")
    printString("\0A")
    jump(endIf)

endIf:
    return
}

func main() {
    call(findPalidrome)
    return
}

init {
    return
}
```

As you can see a lot of the implicit semantics are made explicit, such as: zero values, temporary variable in multiple
assignments, and void returns at the end of functions. All the control flow is re-written as branches and labels.
Most statements are simplified into multiple ones, such as one print per expression, and one declaration per variable.
Operators are also made type specific; for example comparing integers uses `==`, but strings use `==$`. More complex
operations, like struct and array equality are expanded to many lines of code.

All of the non-declaration statements use a new notation in the form of `stmtName(symbols and values)`, which is more
verbose but easier to work with.

Type declarations are ignored, and all types and symbol have their aliases removed. This can make things a bit verbose
when large structures are used, but it's much simpler to work with. It's difficult to keep structure names because Go
doesn't actually have named types, instead it has named aliases to types. We would have to remove all the aliasing
layer except the last one on structs, then create a new named structure type, and update all the references to it. This
is a lot of error prone work, which we opted not to do.

The last function is a special one, which is called before `main`, and is used to initialize the global variables.
Since the program has no global variables, it is empty. Otherwise it would contain assignments.

If the Golite program does not provide a `main` function, then an empty one is generated.

The IR conversion also prepares the control flow for conversion to LLVM IR by removing any constructs that would violate
the validation rules. This mostly means removing code after `return`, `break` and `continue`. The basic blocks are also
always terminated with a `return` or an unconditional jump for the same reason.

This is done with a modification pass after the IR conversion. As an example, here is a valid Golite program that does
not translate directly to LLVM IR:

```go
package main

func abs(i int) int {
    if i < 0 {
        return -i
    } else {
        return i
    }
}

func main() {
    for i := -5; i <= 5; i++ {
        println(abs(i))
    }
}
```

This is the IR without the pass:

```go
package main

func abs(i int) {
    jump(ifCase, (i < 0))
    jump(elseCase)

ifCase:
    return -i
    jump(endIf)

elseCase:
    return i
    jump(endIf)

endIf:
}

func main() {
    var i0 int
    assign(i0, -5)

startFor:
    jump(endFor, !(i0 <= 5))
    printInt(call(abs, i0))
    printString("\0A")
    jump(continueFor)
    printString("skipped")
    printString("\0A")

continueFor:
    assign(i0, (i0 + 1))
    jump(startFor)

endFor:
    return
}

init {
    return
}
```

Notice how the `abs` function has a label without any statements underneath it (an empty basic block)? In LLVM
this would violate the rule that each basic block must end with a terminator (branch, return or exception handling).
Both the `ifCase` and `elseCase` blocks also violate a rule: a terminator must be the last in the block; yet a return
appears before a branch. Same issue in the `main`'s `startFor` block. Another less obvious issue is the implicit
fall-through from `main`'s entry block to `startFor`, which is invalid due to the lack of terminator.

We fix these issues in five steps:
- First we convert the statement list of a function into basic blocks, which are separated by labels
- Then we add a jump to the next block to any block that doesn't end with a terminator
- Next we remove all the statements that come after the first terminator in a block (unreachable)
- The previous step will make some blocks unreachable by removing jumps, so we now remove them
- Lastly we recombine the blocks into a statement list

This work is done by the `IrFlowSanitizer` class.

Now after the pass our output IR is:

```go
package main

func abs(i int) {
    jump(ifCase, (i < 0))
    jump(elseCase)

ifCase:
    return -i

elseCase:
    return i
}

func main() {
    var i0 int
    assign(i0, -5)
    jump(startFor)

startFor:
    jump(endFor, !(i0 <= 5))
    printInt(call(abs, i0))
    printString("\0A")
    jump(continueFor)

continueFor:
    assign(i0, (i0 + 1))
    jump(startFor)

endFor:
    return
}

init {
    return
}
```

As you can see, the `endIf` block was made unreachable and was removed. The unreachable statements in `startFor` were
also removed. The `main`'s first block now explicitly jumps to the next.

It might not be obvious how this transformation solve the problem with the `endIf` block. First we have to observe that
void-returning function never have this issue, since they always end with a `return` statement (we make it explicit).
Thus there always is a statement at the very end of the function. For value-returning functions, we have to remember that
we valid the return paths in the type-checker. This means that there will always be terminator(s) in the path to `endIf`.
Then all we need to do is to remove the unreachable statements until those terminator(s) are the last block(s).

### Runtime

There are a few operation in Golite which are rather complicated to implement in LLVM IR directly. Some of them are simply
too verbose. Instead we can implement them in C (or any other compiled language) to make our lives easier. In the file
`src/main/c/golite_runtime`, we implemented the following functionality: printing to `stdout`, bounds checking, slice
appending, string concatenation, and string comparison.

The printing functionality is implemented in C mostly because:
- Boolean are printed as "true" or "false" instead of 0 or 1
- Strings are not null-terminated in Go, which means that we need to print each character at a time using a loop

The other print functions are just there for consistency.

The remaining operations are implemented in C for readability and conciseness.

Notice that the integer types used have exact sizes. This is for better compatibility with LLVM, which also uses strictly
defined sizes for integers.

Another important declaration in the runtime is the structure type `goliteRtSlice`. It is a length field and a pointer
to a memory buffer. As the name suggests, it is the backing data structure for slices, but is also used for strings. Using
this we can implement the pass-by-reference semantics of slices. String are immutable, so the pass-by semantics do not
matter.

Finally, at the end of the file, are two prototypes: `void staticInit(void)` and `void goliteMain(void)`. These are
implemented by the code generator. The first contains code for initializing global variables, and the second is the
actual Golite `main` entry point. The last declaration of the runtime is the C `main`, which simply calls `staticInit`
followed by `goliteMain`.

### Final conversion to LLVM IR

By using a custom IR, we can significantly reduce the number of different nodes we need to code-generate, and also make
them more compatible with LLVM IR.

The first step in converting a program is to declare the external functions that are implemented in the runtime. We also
declare a named structure type for `goliteRtSlice`.

Next we declare the global variables. Then we can generate the functions one after the other. One important thing here
is to prevent the function in the Golite program for interfering with the pre-defined names for linking with the runtime.
This means that we need to ensure that only the `main` function is called `goliteMain`, and that none have the name
`staticInit`. We can simply append a '1' to the name, and let LLVM figure out any further naming conflicts.

As we traverse the custom IR, we generate the corresponding LLVM IR. Most of this is rather straight forward, and based
off the LLVM documentation.

To code-generate a function, we first declare one basic block per label. This is done to solve the issue of forward
references to labels (like at the end of a loop). Then we position an instruction builder at the end of the current label,
and append instructions.

For every function parameter, we allocate memory on the stack and copy the value into it. We then save a pointer
to the memory corresponding to the variable. This is done so we can assign new values to parameters (which doesn't
change the caller's arguments).

Variable declarations simply allocate stack memory and save the pointer to it.

Boolean, integer and float literals are converted to LLVM constant values. String literals are added to a
constant pool, then a pointer to the constant is taken and is returned with the string length in a `goliteRtSlice`.

Identifiers are simply converted to a pointer to the variable's memory.

Select expressions get a pointer to the field inside the struct.

Index expressions are a bit more complicated: first we compute a pointer to the value and the index. Then we need to find
the length of the data. For an array, we just use the type, since it is constant. For slices, we need to access the length
field in the struct. Next we call the bounds checking function in the runtime. Finally we can get a pointer in the memory
are the index.

Note how the identifier, select and index expressions return pointers instead of values. This is because these expressions
are assignable. This means that we need a reference to the memory into which a value should be copied. But we also need
to use them as values in many cases. Sometimes we also want to have values as pointers instead (for passing the data to
the slice append function for example). To do this we use access values and pointers through the `getExprValue()` and
`getExprPtr()` methods. These take care of loading a pointer when trying to access it as a value, and of storing a value
on the stack when trying to access a value as a pointer.

The append expression will compute the size of the item being appended in bytes, then will store it in the stack. It
then calls the runtime function with the slice, data pointer and size. The returned value is a slice pointing to
a new memory buffer, which contains the original slice data followed by the appended data. This is really a concatenation
of one item.

String concatenation is basically the same, since strings also use slices, and the runtime function for appending can
also do concatenation.

String comparison is simply done by calling a runtime function. We pass an integer ID for the kind of comparison, and both
slices.

The logical AND and OR are a lot more complicated than the other operators because of short-circuiting. We need to
generate two new basic blocks, and insert them in the proper location. This also implies updating the instruction builder
to append at the correct block. The basic idea it to compute the left value first; then branch to the end, or compute
the right value then branch to the end; then return the value based on which branch we took. We could do this with a
phi node, but it's quite difficult to do when the left or right is a global variable. This is because we need to specify
which basic block the value originates from, yet globals don't belong to any. Instead it's much simpler to allocate stack
memory and store the intermediate results there. Later we will run optimization passes and let LLVM convert this to a phi
instruction.

The remaining expression all translate directly to a single LLVM instruction. With similar names and semantics.

Return statements also translate to a single instruction.

The print statements are simply converted to a call to the runtime, with the value as an argument.

The memset statement is converted to a call to the LLVM memset intrinsic. The first argument is a pointer to
the data being cleared. The second is the size to clear. In our case, the data is a variable pointer, and the size is
given by the type. There's a trick to compute this size: create a null pointer to the variable type, then treat it as an
array and get a pointer to the second element. Now we just convert this to an int. Since the pointer is null, the first
index is `0`, and the second is `sizeof(type)`. When optimizing, LLVM will replace these instructions by a constant
integer, taking care of calculating the data size and padding for us.

Assignments are simply converted to storing the right side value into the memory pointed by the left side.

Labels were converted to basic blocks earlier. Now when we encounter one, we just have to move the instruction builder
to the block, and start appending instruction there.

An unconditional jump is just a branch in LLVM. A conditional one is also a branch, but it's a bit different. Our IR only
specifies the `true` destination, the `false` one being the instruction right after the jump. LLVM requires both. This
can be solved by adding a new basic block after the jump, using it as the `false` destination.

The very last step if to apply optimization passes to the generated LLVM IR. We picked the following ones: constant
propagation, instruction combination, memory-to-register promotion, global value numbering (redundant instruction
removal), and control-flow graph simplification. This is a safe and rather basic set of optimizations.

## Conclusion

## Contributions

Aleksi Sapon took care of the project lead, architecture and design. He also implemented symbols, types and contexts,
and most of the conversion from the custom IR to LLVM IR. Otherwise he also helped with every other aspect of the compiler.
Finally he took care of the overall code quality and consistency.
