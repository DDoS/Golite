# Milestone 3

## Implementation

We have chosen to implement Milestone 4 using low-level code, specifically, LLVM-IR. More specifically, 
we plan to first convert Golite to a simpler IR and thereby convert this IR to a LLVM IR representation,
specifically using the LLVM C API. We plan to use LLVM because of the several advantages it offers over 
other approaches. Firstly, LLVM libraries are designed to facilitate re-usability and extendibility with
LLVM supporting several languages including C and C++. Secondly, LLVM IR enables a clean seperation of the
compiler frontend and backend. This enables easy swap of a new frontend/backend due to the same LLVM 
intermediate representation.E.g., with this independent IR, a C-language front-end can be reused regardless 
of the targeted object code. Similarly, another back-end could be reused similarly with another programming
language enabling implementation of a compiler pipeline architecture. Also, LLVM IR supports different features
for different languages. It is also possible to create new optimization passes with minimal code changes to 
existing cose. Finally, LLVM IR is comparitively easy to work with and due to its popularity is well documented
and standardized.

It has some disadvantages in that it is comparitively more difficulat to implement than higher level languages
like C/C++ but we wanted to take on this challenging task and in the process learn more about LLVM IR 
implementation. It also supports a much smaller set of languages compared to GCC (IR) but we are not 
concerned with the extendibility for other languages, so we can chose to ignore it.

### Test Programs

#### MultiAssignSwap.go :
It's a good edge case for Code Generation. a, b = b, a should swap variables, which won't work unless you 
codegenerate intermediate variables for the values. 
#### switch_codegen.go: 
Checks if the generated code is able to handle the comparison condition in a case conditional. Many languages 
like C don't support these kind of Case conditions by default, so it is an interesting case.
#### switch_codegen_booleans.go:
This again checks if the generated code is able to handle two booleans in a case conditional. Many languages
like C don't support these kind of case conditions by default, so it is an interesting case as well.
#### array_default.go :
It's a good test case to check if an array is initialized during the codegen by default. We used a multidimentional 
float array to check this.
#### scope_var.go: 
This program is to ensure that the scopes are handled currectly by the codegen. The value of the variable outside
the function scope should be replaced by the newly declared in-scope value.
