/*
    An instruction set for a very basic CPU based on ARMv4

    Registers:
        R0 - R12: general purpose
        LR (R13): link register
        SP (R14): stack pointer
        PC (R15): program counter

    Condition codes:
        0 (EQ): equal
        1 (NE): not equal
        2 (HS): unsigned higher or same
        3 (LO): unsigned lower
        4 (MI): negative
        5 (PL): positive
        6 (VS): overflow set
        7 (VC): overflow clear
        8 (HI): unsigned higher
        9 (LS): unsigned lower or same
        A (GE): greater or equal to
        B (LT): lesser than
        C (GT): greater than
        D (LE): lesser or equal to
        E (AL): always
        F (NV): never

    Program status flags:
        N: negative flag
        Z: zero flag
        C: carry flag
        V: overflow flag
        R: running

    Instructions:
        MOV Rm Rd          : Rd = Rm <update NZ>
        MOV Imm Rd         : Rd = Imm <update NZ>
        AND Rm Rn Rd       : Rd = Rm & Rn <update NZ>
        ORR Rm Rn Rd       : Rd = Rm | Rn <update NZ>
        EOR Rm Rn Rd       : Rd = Rm ^ Rn <update NZ>
        SUB Rm Rn Rd       : Rd = Rm - Rn <update NZCV>
        ADD Rm Rn Rd       : Rd = Rm + Rn <update NZCV>
        CMP Rm Rn          : Rm - Rn <update NZCV>
        MUL Rm Rn Rd       : Rd = Rm * Rn <update NZ>
        DIV Rm Rn Rd       : Rd = Rm / Rn <update NZ>
        REM Rm Rn Rd       : Rd = Rm % Rn <update NZ>
        LSL Rm Rn Rd       : Rd = Rm << Rn <update NZC>
        LSR Rm Rn Rd       : Rd = Rm >>> Rn <update NZC>
        ASR Rm Rn Rd       : Rd = Rm >> Rn <update NZC>
        B   Ra             : PC = Ra
        B   Imm            : PC = Imm
        STR Rs Ra          : memory[Ra] = Rs
        LDR Ra Rd          : Rd = memory[Ra]
        POP [R0 - R12, PC] : pop resiters on the stack
        PUSH [R0 - R12, LR]: push registers of the stack
        STOP               : <clear R>

    Encoding format:
        31 - 28: condition code
        27 - 20: op code
        19 - 0 : operands

    Instruction encodings:
        MOV Rm Rd          : c 00 ...md
        MOV Imm Rd         : c 01 iiiid
        AND Rm Rn Rd       : c 02 ..mnd
        ORR Rm Rn Rd       : c 03 ..mnd
        EOR Rm Rn Rd       : c 04 ..mnd
        SUB Rm Rn Rd       : c 05 ..mnd
        ADD Rm Rn Rd       : c 06 ..mnd
        CMP Rm Rn          : c 14 ...mn
        MUL Rm Rn Rd       : c 07 ..mnd
        DIV Rm Rn Rd       : c 08 ..mnd
        REM Rm Rn Rd       : c 09 ..mnd
        LSL Rm Rn Rd       : c 0A ..mnd
        LSR Rm Rn Rd       : c 0B ..mnd
        ASR Rm Rn Rd       : c 0C ..mnd
        B   Ra             : c 0D ....a
        B   Imm            : c 0E iiiii
        STR Rs Ra          : c 0F ...sa
        LDR Ra Rd          : c 10 ...ad
        POP [R0 - R12, PC] : c 11 .llll
        PUSH [R0 - R12, LR]: c 12 .llll
        STOP               : c 13 .....
*/

package main

var registers [16]int

var N, Z, C, V, R bool

var ram [1024]int
var rom [256]int

var (
    R0 = 0; R1 = 1; R2 = 2; R3 = 3; R4 = 4; R5 = 5; R6 = 6; R7 = 7
    R8 = 8; R9 = 9; R10 = 10; R11 = 11; R12 = 12;
    LR = 13; SP = 14; PC = 15
)

func memRead(address int) int {
    // The high nibble maps the address to ROM or RAM
    switch (address >> 28) & 0xF {
    case 0:
        return rom[address & 0xFFFFFFF]
    case 1:
        return ram[address & 0xFFFFFFF]
    }
    // Invalid addresses default to 0
    return 0
}

func memWrite(address, value int) {
    // The high nibble maps the address to ROM or RAM
    switch (address >> 28) & 0xF {
    case 0:
        // Ignore write on the ROM
    case 1:
        ram[address & 0xFFFFFFF] = value
    }
}

func checkCondition(cond int) bool {
    switch cond {
    case 0x0: return Z // EQ
    case 0x1: return !Z // NE
    case 0x2: return C // HS
    case 0x3: return !C // LO
    case 0x4: return N // MI
    case 0x5: return !N // PL
    case 0x6: return V // VS
    case 0x7: return !V // VC
    case 0x8: return C && !Z // HI
    case 0x9: return !C || Z // LS
    case 0xA: return N == V // GE
    case 0xB: return N != V // LT
    case 0xC: return !Z && N == V // GT
    case 0xD: return Z || N != V // LE
    case 0xE: return true // AL
    case 0xF: return false // NV
    }
    return false
}

func updateNZ(value int) {
    N = value < 0
    Z = value == 0
}

func carriedAdd(a, b, c int) bool {
    negativeA := a >> 31 != 0
    negativeB := b >> 31 != 0
    negativeC := c >> 31 != 0
    return negativeA && negativeB || negativeA && !negativeC || negativeB && !negativeC
}

func overflowedAdd(a, b, c int) bool {
    negativeA := a >> 31 != 0
    negativeB := b >> 31 != 0
    negativeC := c >> 31 != 0
    return negativeA && negativeB && !negativeC || !negativeA && !negativeB && negativeC
}

func borrowedSub(a, b, c int) bool {
    negativeA := a >> 31 != 0
    negativeB := b >> 31 != 0
    negativeC := c >> 31 != 0
    return (!negativeA || negativeB) && (!negativeA || negativeC) && (negativeB || negativeC)
}

func overflowedSub(a, b, c int) bool {
    negativeA := a >> 31 != 0
    negativeB := b >> 31 != 0
    negativeC := c >> 31 != 0
    return negativeA && !negativeB && !negativeC || !negativeA && negativeB && negativeC
}

func aluMovReg(inst int) {
    Rm := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm]
    registers[Rd] = value
    updateNZ(value)
}

func aluMovImm(inst int) {
    Imm := (inst >> 4) & 0xFFFF
    Rd := inst & 0xF
    registers[Rd] = Imm
    updateNZ(Imm)
}

func aluAnd(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm] & registers[Rn]
    registers[Rd] = value
    updateNZ(value)
}

func aluOrr(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm] | registers[Rn]
    registers[Rd] = value
    updateNZ(value)
}

func aluEor(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm] ^ registers[Rn]
    registers[Rd] = value
    updateNZ(value)
}

func aluSub(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    op1 := registers[Rm]
    op2 := registers[Rn]
    value := op1 - op2
    registers[Rd] = value
    updateNZ(value)
    C = !borrowedSub(op1, op2, value)
    V = overflowedSub(op1, op2, value)
}

func aluAdd(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    op1 := registers[Rm]
    op2 := registers[Rn]
    value := op1 + op2
    registers[Rd] = value
    updateNZ(value)
    C = carriedAdd(op1, op2, value)
    V = overflowedAdd(op1, op2, value)
}

func aluCmp(inst int) {
    Rm := (inst >> 4) & 0xF
    Rn := inst & 0xF
    op1 := registers[Rm]
    op2 := registers[Rn]
    value := op1 - op2
    updateNZ(value)
    C = !borrowedSub(op1, op2, value)
    V = overflowedSub(op1, op2, value)
}

func aluMul(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm] * registers[Rn]
    registers[Rd] = value
    updateNZ(value)
}

func aluDiv(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm] / registers[Rn]
    registers[Rd] = value
    updateNZ(value)
}

func aluRem(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    value := registers[Rm] % registers[Rn]
    registers[Rd] = value
    updateNZ(value)
}

func aluLsl(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    op := registers[Rm]
    sh := registers[Rn]
    value := op << sh
    registers[Rd] = value
    updateNZ(value)
    if sh == 0 {
        // No change
    } else if sh > 0 && sh <= 32 {
        C = (op >> (32 - sh)) & 0x1 == 0x1
    } else {
        C = false
    }
}

func aluLsr(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    op := registers[Rm]
    sh := registers[Rn]
    value := op >> sh
    // No logical right shift in golite, so do it by hand
    if sh > 0 {
        value &= ^(0x80000000 >> (sh - 1))
    }
    registers[Rd] = value
    updateNZ(value)
    if sh == 0 {
        // No change
    } else if sh > 0 && sh <= 32 {
        C = (op >> (sh - 1)) & 0x1 == 0x1
    } else {
        C = false
    }
}

func aluAsr(inst int) {
    Rm := (inst >> 8) & 0xF
    Rn := (inst >> 4) & 0xF
    Rd := inst & 0xF
    op := registers[Rm]
    sh := registers[Rn]
    value := op >> sh
    registers[Rd] = value
    updateNZ(value)
    if sh == 0 {
        // No change
    } else if sh > 0 && sh <= 32 {
        C = (op >> (sh - 1)) & 0x1 == 0x1
    } else if sh > 32 {
        C = (op >> 31) & 0x1 == 0x1
    } else {
        C = false
    }
}

func branchReg(inst int) {
    Ra := inst & 0xF
    registers[PC] = registers[Ra]
}

func branchImm(inst int) {
    Imm := inst & 0xFFFFF
    registers[PC] = Imm
}

func memStore(inst int) {
    Rs := (inst >> 4) & 0xF
    Ra := inst & 0xF
    memWrite(registers[Ra], registers[Rs])
}

func memLoad(inst int) {
    Ra := (inst >> 4) & 0xF
    Rd := inst & 0xF
    registers[Rd] = memRead(registers[Ra])
}

func memPop(inst int) {
    list := inst & 0xFFFF
    sp := registers[SP]
    for i := 0; i <= 12; i++ {
        if (list >> i) & 0x1 == 0x1 {
            registers[i] = memRead(sp)
            sp += 4
        }
    }
    if (list >> PC) & 0x1 == 0x1 {
        registers[PC] = memRead(sp)
        sp += 4
    }
    registers[SP] = sp
}

func memPush(inst int) {
    list := inst & 0xFFFF
    sp := registers[SP]
    if (list >> LR) & 0x1 == 0x1 {
        sp -= 4
        memWrite(sp, registers[LR])
    }
    for i := 12; i >= 0; i++ {
        if (list >> i) & 0x1 == 0x1 {
            sp -= 4
            memWrite(sp, registers[i])
        }
    }
    registers[SP] = sp
}

func statusStop(inst int) {
    R = false
}

func execute(inst int) {
    // Check that the condition is valid
    if cond := (inst >> 28) & 0xF; !checkCondition(cond) {
        return
    }
    // Dispatch to the proper instruction executor
    switch op := (inst >> 20) & 0xFF; op {
    case 0x0: aluMovReg(inst)
    case 0x1: aluMovImm(inst)
    case 0x2: aluAnd(inst)
    case 0x3: aluOrr(inst)
    case 0x4: aluEor(inst)
    case 0x5: aluSub(inst)
    case 0x6: aluAdd(inst)
    case 0x14: aluCmp(inst)
    case 0x7: aluMul(inst)
    case 0x8: aluDiv(inst)
    case 0x9: aluRem(inst)
    case 0xA: aluLsl(inst)
    case 0xB: aluLsr(inst)
    case 0xC: aluAsr(inst)
    case 0xD: branchReg(inst)
    case 0xE: branchImm(inst)
    case 0xF: memStore(inst)
    case 0x10: memLoad(inst)
    case 0x11: memPop(inst)
    case 0x12: memPush(inst)
    case 0x13: statusStop(inst)
    }
}

func main() {
    // Place a progam in the ROM
    // This one has been assembled by asm.go
    // Take a look at the code() func for details
    rpc := 0
    rom[rpc] = -535822317 ; rpc++
    rom[rpc] = -515899389 ; rpc++
    rom[rpc] = -1059061755 ; rpc++
    rom[rpc] = -535822320 ; rpc++
    rom[rpc] = -522190835 ; rpc++
    rom[rpc] = -535822319 ; rpc++
    rom[rpc] = -535822302 ; rpc++
    rom[rpc] = -529530591 ; rpc++
    rom[rpc] = -515899360 ; rpc++
    rom[rpc] = -1595932660 ; rpc++
    rom[rpc] = -530578894 ; rpc++
    rom[rpc] = -522190841 ; rpc++
    rom[rpc] = -536870896 ; rpc++
    rom[rpc] = -516947968 ; rpc++
    // Set the argument to the program, which will calculate the factorial of R0
    registers[R0] = 6
    // Initialize the program counter to the begining of memory
    registers[PC] = 0
    // Initialize the stack pointer to the top of the memory
    registers[SP] = 1024
    // Set the vm to running
    R = true
    // Run the CPU loop until the program is stopped
    for R {
        // Fetch the instruction from memory
        oldPc := registers[PC]
        inst := memRead(oldPc)
        // Execute it
        execute(inst)
        // Increment the instruction pointer if not changed (else branch)
        newPc := registers[PC]
        if (oldPc == newPc) {
            registers[PC]++
        }
    }
    // By convention, the program output will be in R0
    println(registers[R0])
}
