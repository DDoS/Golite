# Milestone 1

## Tools and languages

We chose SableCC as a compiler-compiler. This is because most team
members had used it for assignment 1, and because it was explained
in class. We used the latest stable version: 3.7.

Using SableCC limits us into using a JVM language. Again, based on
previous experience, we chose Java. We used version 8 since it is
now widely supported and offers a lot of useful features over
previous versions. 

To simplify building across Linux, mac OS and Windows, we opted to
use Gradle as a build tool. We used a plugin to integrate SableCC
as part of the build process. The only caveat is that we had to
make our own plugin, and so it must be bundled with the project.
Otherwise, Gradle downloads dependencies, compiles, tests and runs
the project with a few simple commands. Gradle doesn't need to be
installed on the machine either, thanks to the Gradle Wrapper.

Other than SableCC, the project depends Apache Commons CLI for the
command line application, and on JUnit for testing.

## Implementation

Stuff here

## Team work summary

Aleksi Sapon set up the project structure. He implemented the
command line application and test runner. He contributed parts of
the pretty printer and weeder. He wrote the entire expression
grammar, and part of the declaration one. As for test cases, he
wrote two valid programs, ten invalid ones, and a few more for the
extra tests. Finally he also took care of overall code quality and
consistency.
