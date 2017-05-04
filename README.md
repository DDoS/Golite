# Golite compiler

McGill COMP 520 - Compiler Design, Winter 2017.

Golite project source code for team 9.

Licensed under [MIT](LICENSE.txt).

## Team 9 members

[Aleksi Sapon-Cousineau](https://github.com/DDoS)  
[Rohit Verma](https://github.com/RohitVMcGill)  
[Ayesha Krishnamurthy](https://github.com/ayeshakr)

## Golite

Golite is mostly a subset of Go developed for the COMP 520 course.
You can find example programs in the `programs` directory.

## Dependencies

All dependencies are managed by Gradle, including LLVM 3.9.1.
They will be downloaded for the first build.

## Building

Use `./build.sh` for the first build. Afterwards it's better to
use `gradlew` for incremental builds.

## Runtime

The Golite built-ins are implemented as a few simple C functions,
which are linked in during the last phase of compilation. The
build system has a task to compile the runtime into an object,
which can be found at `build/objs/golite_runtime.o`. It must
be linked into the object file resulting from the code generation
phase to create the final executable.

We provide a script to do all of this automatically. It is detailed
bellow.

The runtime source can be found along side the Java code, under
`src/main/c`.

## Running

Use the `run.sh` script to invoke Gradle and pass the command line arguments.

## Command line usage

Run with the `-h` option for help.
