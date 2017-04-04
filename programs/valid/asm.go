/*
    See "vm.go" for a description of the instruction set
*/

package main

var (
    EQ = 0x0; NE = 0x1; HS = 0x2; LO = 0x3; MI = 0x4; PL = 0x5; VS = 0x6; VC = 0x7
    HI = 0x8; LS = 0x9; GE = 0xA; LT = 0xB; GT = 0xC; LE = 0xD; AL = 0xE; NV = 0xF
)

var (
    R0 = 0; R1 = 1; R2 = 2; R3 = 3; R4 = 4; R5 = 5; R6 = 6; R7 = 7
    R8 = 8; R9 = 9; R10 = 10; R11 = 11; R12 = 12; LR = 13; SP = 14; PC = 15
)

var (
    R0bit = 1 << 0; R1bit = 1 << 1; R2bit = 1 << 2; R3bit = 1 << 3
    R4bit = 1 << 4; R5bit = 1 << 5; R6bit = 1 << 6; R7bit = 1 << 7
    R8bit = 1 << 8; R9bit = 1 << 9; R10bit = 1 << 10; R11bit = 1 << 11
    R12bit = 1 << 12; LRbit = 1 << 13; SPbit = 1 << 14; PCbit = 1 << 15
)

type LInst struct {
    inst int
    label, targetLabel string
}

var lInsts []LInst
var lInstCount int
var nextLabel string

func nextInst(inst int) {
    var lInst LInst;
    lInst.inst = inst
    lInst.label = nextLabel
    lInsts = append(lInsts, lInst)
    lInstCount++
    nextLabel = ""
}

func header(cond, op int) int {
    return cond & 0xF << 28 | op << 20
}

func MOVreg(cond, Rm, Rd int) {
    nextInst(header(cond, 0x0) | Rm & 0xF << 4 | Rd & 0xF)
}

func MOVimm(cond, imm, Rd int) {
    nextInst(header(cond, 0x1) | imm & 0xFFFF << 4 | Rd & 0xF)
}

func alu(cond, op, Rm, Rn, Rd int) int {
    return header(cond, op) | Rm & 0xF << 8 | Rn & 0xF << 4 | Rd & 0xF
}

func AND(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x2, Rm, Rn, Rd))
}

func ORR(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x3, Rm, Rn, Rd))
}

func EOR(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x4, Rm, Rn, Rd))
}

func SUB(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x5, Rm, Rn, Rd))
}

func ADD(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x6, Rm, Rn, Rd))
}

func CMP(cond, Rm, Rn int) {
    nextInst(header(cond, 0x14) | Rm & 0xF << 4 | Rn & 0xF)
}

func MUL(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x7, Rm, Rn, Rd))
}

func DIV(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x8, Rm, Rn, Rd))
}

func REM(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0x9, Rm, Rn, Rd))
}

func LSL(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0xA, Rm, Rn, Rd))
}

func LSR(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0xB, Rm, Rn, Rd))
}

func ASR(cond, Rm, Rn, Rd int) {
    nextInst(alu(cond, 0xC, Rm, Rn, Rd))
}

func Breg(cond, Ra int) {
    nextInst(header(cond, 0xD) | Ra & 0xF)
}

func Bimm(cond, imm int) {
    nextInst(header(cond, 0xE) | imm & 0xFFFFF)
}

func STR(cond, Rs, Ra int) {
    nextInst(header(cond, 0xF) | Rs & 0xF << 4 | Ra & 0xF)
}

func LDR(cond, Ra, Rd int) {
    nextInst(header(cond, 0x10) | Ra & 0xF << 4 | Rd & 0xF)
}

func POP(cond, list int) {
    nextInst(header(cond, 0x11) | list & 0xFFFF)
}

func PUSH(cond, list int) {
    nextInst(header(cond, 0x12) | list & 0xFFFF)
}

func STOP(cond int) {
    nextInst(header(cond, 0x13))
}

func B(cond int, label string) {
    Bimm(cond, 0)
    lInsts[lInstCount - 1].targetLabel = label
}

func ADR(cond, Rd int, label string) {
    MOVimm(cond, 0, Rd)
    lInsts[lInstCount - 1].targetLabel = label
}

func NOP() {
    MOVreg(NV, 0, 0)
}

func label(label string) {
    nextLabel = label
}

func addressAtLabel(label string) int {
    for i := 0; i < lInstCount; i++ {
        if lInsts[i].label == label {
            return i
        }
    }
    return -1
}

func replaceImm(inst, imm int) int {
    switch inst >> 20 & 0xFF {
    case 0xE:
        // B imm
        inst &= ^0xFFFFF
        inst = inst | imm & 0xFFFFF
    case 0x1:
        // MOV imm
        inst &= ^(0xFFFF << 4)
        inst = inst | imm & 0xFFFF << 4
    }
    return inst
}

func assemble() int {
    // Resolve labels by replacing them by addresses
    for i := 0; i < lInstCount; i++ {
        // Find an instruction with a target label to resolve
        targetLabel := lInsts[i].targetLabel;
        if targetLabel == "" {
            continue
        }
        // Find the address of the instruction with the label
        address := addressAtLabel(targetLabel)
        if address < 0 {
            println("Error for instruction ", i, ": label ", targetLabel, " not declared")
            return -1
        }
        // Set the address as the immediate operand in the instruction
        lInsts[i].inst = replaceImm(lInsts[i].inst, address)
    }
    return 0
}

func code() {
    // Parameter: R0 = n
    MOVimm(AL, 1, R3) // const R3 = 1

    CMP(AL, R0, R3)
    B(GT, "factN") // if n > 1: goto factN

    MOVimm(AL, 1, R0)
    B(AL, "end") // else return  1

label("factN")
    MOVimm(AL, 1, R1) // fact = 1
    MOVimm(AL, 2, R2) // i = 2
label("loop")
    MUL(AL, R1, R2, R1) // fact = fact * i

    CMP(AL, R2, R0)
    B(GE, "endLoop") // if i >= n: goto endLoop

    ADD(AL, R2, R3, R2) // i += 1
    B(AL, "loop") // goto loop

label("endLoop")
    MOVreg(AL, R1, R0) // return fact

label("end")
    STOP(AL)
}

func main() {
    if code(); assemble() == 0 {
        println("rpc := 0")
        for i := 0; i < lInstCount; i++ {
            println("rom[rpc] = ", lInsts[i].inst, "; rpc++")
        }
    }
}
