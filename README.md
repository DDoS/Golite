# Golite compiler

## Team members

Aleksi Sapon-Cousineau - 260581670
Rohit Verma - 260710711
Ayesha Krishnamurthy - 260559114

## Project structure

We followed the required project structure. Additionally, we
used Gradle to manage building. This means that we have extra
files in the project root to configure the tool. We also have
a couple of `gradlew` scripts to run the build without requiring
a Gradle installation. The `gradle` directory contains the wrapper
and a plugin for SableCC support.

We decided to bundle our extra test cases. They can be found in
the directories `programs/valid_extra`, `programs/invalid_extra`
and `programs/code_extra`.

## Dependencies

All dependencies are managed by Gradle, including LLVM 3.9.1.
They will be downloaded for the first build.

## Building

Use `build.sh` for the first build. Afterwards it's better to
use `gradlew` for incremental builds.

## Running

After building, a `run.sh` file will be created. It can be used to
run the compiler directly, without waiting for Gradle. If you want
to automatically rebuild before each run, use `run_gradle.sh`.

The `run.sh` script is configured to execute the `codegen` command.

## Command line usage

Run with the `-h` option for help.

## LLVM compatibility issues

Our project uses LLVM 3.9.1, which is the only version we could find
with up-to-date working Java bindings. The `ubuntu.cs.mcgill.ca` server
provides LLVM 3.8.0, which isn't fully compatible. This isn't an issue
for running our compiler, since the bindings provide their own binaries
instead of using the system ones.

The problem is with compiling the LLVM IR that the `codegen` command outputs.
It is not compatible between the two versions. Fortunately, since we provide
the 3.9.1 binaries, we can use those instead. We provide an `compile.sh`
script, which can input LLVM IR in text or bitcode form, and output an executable.
It takes care of linking with the runtime.

Alternatively, you can use the `compile` command to go from a Golite source file
straight to an executable. But since the `run.sh` script uses the codegen command,
you will have to modify it. All that needs to be changed is the `GOLITE_CMD` variable.
