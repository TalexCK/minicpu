from .utils import read_file_as_list, write_file
import re


class AssemblerType:
    def __init__(self, **kwargs):
        for key, value in kwargs.items():
            setattr(self, key, value)


pc = 0x00000000
list_label_name = []
list_label_pc = []

reg_alias = {
    "zero": 0, "ra": 1,  "sp": 2,  "gp": 3,  "tp": 4,
    "t0": 5,   "t1": 6,  "t2": 7,
    "s0": 8,   "fp": 8,  "s1": 9,
    "a0": 10,  "a1": 11, "a2": 12, "a3": 13, "a4": 14, "a5": 15, "a6": 16, "a7": 17,
    "s2": 18,  "s3": 19, "s4": 20, "s5": 21, "s6": 22, "s7": 23, "s8": 24, "s9": 25, "s10": 26, "s11": 27,
    "t3": 28,  "t4": 29, "t5": 30, "t6": 31,
}

alias = re.compile(r"\b(" + "|".join(map(re.escape, reg_alias.keys())) + r")\b", re.IGNORECASE)


# Types
class RType(AssemblerType):
    opcode: int
    funct3: int
    funct7: int

    def encode(self, args: list):
        opcode = self.opcode
        funct3 = self.funct3
        funct7 = self.funct7
        rd = int(args[0][1:], 0)
        rs1 = int(args[1][1:], 0)
        rs2 = int(args[2][1:], 0)

        return (
                to_bin(funct7, 7)
                + to_bin(rs2, 5)
                + to_bin(rs1, 5)
                + to_bin(funct3, 3)
                + to_bin(rd, 5)
                + to_bin(opcode, 7)
        )


class IType(AssemblerType):
    opcode: int
    funct3: int
    imm: int = 0
    imm_start: int = 0x0
    funct7: int = 0x00
    if_funct7: bool = False

    def encode(self, args: list):
        opcode = self.opcode
        if opcode == 0x73:
            funct7 = self.funct7
            if funct7 == 0x0:
                return "00000000000000000000000001110011"
            else:
                return "00000000000100000000000001110011"
        elif opcode == 0x03:
            opcode = self.opcode
            funct3 = self.funct3
            rd = int(args[0][1:], 0)
            rs1_and_imm = list(args[1].split("("))
            rs1 = int(rs1_and_imm[1][1:-1], 0)
            try:
                imm = int(rs1_and_imm[0], 0)
            except ValueError:
                imm = list_label_pc[list_label_name.index(rs1_and_imm[0])]
            return (
                    to_bin(imm, 12)
                    + to_bin(rs1, 5)
                    + to_bin(funct3, 3)
                    + to_bin(rd, 5)
                    + to_bin(opcode, 7)
            )
        elif opcode == 0x67:
            opcode = self.opcode
            funct3 = self.funct3
            rd = int(args[0][1:], 0)
            rs1_and_imm = list(args[1].split("("))
            rs1 = int(rs1_and_imm[1][1:-1], 0)
            imm = int(rs1_and_imm[0], 0)
            return (
                    to_bin(imm, 12)
                    + to_bin(rs1, 5)
                    + to_bin(funct3, 3)
                    + to_bin(rd, 5)
                    + to_bin(opcode, 7)
            )
        else:
            funct3 = self.funct3
            rd = int(args[0][1:], 0)
            rs1 = int(args[1][1:], 0)
            if_funct7 = self.if_funct7
            imm = int(args[2], 0)
            if if_funct7:
                funct7 = self.funct7
                return (
                        to_bin(funct7, 7)
                        + to_bin(imm, 5)
                        + to_bin(rs1, 5)
                        + to_bin(funct3, 3)
                        + to_bin(rd, 5)
                        + to_bin(opcode, 7)
                )
            else:
                return (
                        to_bin(imm, 12)
                        + to_bin(rs1, 5)
                        + to_bin(funct3, 3)
                        + to_bin(rd, 5)
                        + to_bin(opcode, 7)
                )


class SType(AssemblerType):
    opcode: int
    funct3: int

    def encode(self, args: list):
        opcode = self.opcode
        funct3 = self.funct3
        rs2 = int(args[0][1:], 0)
        rs1_and_imm = list(args[1].split("("))
        rs1 = int(rs1_and_imm[1][1:-1], 0)
        imm = int(rs1_and_imm[0], 0)

        return (
                to_bin(imm, 7, 5)
                + to_bin(rs2, 5)
                + to_bin(rs1, 5)
                + to_bin(funct3, 3)
                + to_bin(imm, 5)
                + to_bin(opcode, 7)
        )


class BType(AssemblerType):
    opcode: int
    funct3: int

    def encode(self, args: list):
        opcode = self.opcode
        funct3 = self.funct3
        rs1 = int(args[0][1:], 0)
        rs2 = int(args[1][1:], 0)
        try:
            imm = int(args[2], 0) - pc
        except ValueError:
            imm = list_label_pc[list_label_name.index(args[2])] - pc

        return (
                to_bin(imm, 1, 12)
                + to_bin(imm, 6, 5)
                + to_bin(rs2, 5)
                + to_bin(rs1, 5)
                + to_bin(funct3, 3)
                + to_bin(imm, 4, 1)
                + to_bin(imm, 1, 11)
                + to_bin(opcode, 7)
        )


class UType(AssemblerType):
    opcode: int

    def encode(self, args: list):
        opcode = self.opcode
        rd = int(args[0][1:], 0)
        try:
            imm = int(args[1], 0)
        except ValueError:
            imm = list_label_pc[list_label_name.index(args[1])]
        if opcode == 0x17:
            imm = imm - pc
        return to_bin(imm, 20) + to_bin(rd, 5) + to_bin(opcode, 7)


class JType(AssemblerType):
    opcode: int

    def encode(self, args: list):
        opcode = self.opcode
        rd = int(args[0][1:], 0)
        try:
            imm = int(args[1], 0) - pc
        except ValueError:
            imm = list_label_pc[list_label_name.index(args[1])] - pc

        return (
                to_bin(imm, 1, 20)
                + to_bin(imm, 10, 1)
                + to_bin(imm, 1, 11)
                + to_bin(imm, 8, 12)
                + to_bin(rd, 5)
                + to_bin(opcode, 7)
        )


InstMap = {
    "add": RType(opcode=0x33, funct3=0x0, funct7=0x00),
    "sub": RType(opcode=0x33, funct3=0x0, funct7=0x20),
    "sll": RType(opcode=0x33, funct3=0x1, funct7=0x00),
    "slt": RType(opcode=0x33, funct3=0x2, funct7=0x00),
    "sltu": RType(opcode=0x33, funct3=0x3, funct7=0x00),
    "xor": RType(opcode=0x33, funct3=0x4, funct7=0x00),
    "srl": RType(opcode=0x33, funct3=0x5, funct7=0x00),
    "sra": RType(opcode=0x33, funct3=0x5, funct7=0x20),
    "or": RType(opcode=0x33, funct3=0x6, funct7=0x00),
    "and": RType(opcode=0x33, funct3=0x7, funct7=0x00),
    "addi": IType(opcode=0x13, funct3=0x0),
    "slti": IType(opcode=0x13, funct3=0x2),
    "sltiu": IType(opcode=0x13, funct3=0x3),
    "xori": IType(opcode=0x13, funct3=0x4),
    "ori": IType(opcode=0x13, funct3=0x6),
    "andi": IType(opcode=0x13, funct3=0x7),
    "slli": IType(opcode=0x13, funct3=0x1, funct7=0x00, if_funct7=True),
    "srli": IType(opcode=0x13, funct3=0x5, funct7=0x00, if_funct7=True),
    "srai": IType(opcode=0x13, funct3=0x5, funct7=0x20, if_funct7=True),
    "lb": IType(opcode=0x03, funct3=0x0),
    "lh": IType(opcode=0x03, funct3=0x1),
    "lw": IType(opcode=0x03, funct3=0x2),
    "lbu": IType(opcode=0x03, funct3=0x4),
    "lhu": IType(opcode=0x03, funct3=0x5),
    "jalr": IType(opcode=0x67, funct3=0x0),
    "ecall": IType(opcode=0x73, funct3=0x0, funct7=0x0),
    "ebreak": IType(opcode=0x73, funct3=0x0, funct7=0x1),
    "sb": SType(opcode=0x23, funct3=0x0),
    "sh": SType(opcode=0x23, funct3=0x1),
    "sw": SType(opcode=0x23, funct3=0x2),
    "beq": BType(opcode=0x63, funct3=0x0),
    "bne": BType(opcode=0x63, funct3=0x1),
    "blt": BType(opcode=0x63, funct3=0x4),
    "bge": BType(opcode=0x63, funct3=0x5),
    "bltu": BType(opcode=0x63, funct3=0x6),
    "bgeu": BType(opcode=0x63, funct3=0x7),
    "lui": UType(opcode=0x37),
    "auipc": UType(opcode=0x17),
    "jal": JType(opcode=0x6F),
}


# transform number to binary string
def to_bin(num: int, length: int, start: int = 0):
    if num < 0:
        return (bin(num & 0xFFFFFFFF)[2:].zfill(32))[32 - start - length: 32 - start]
    return bin(num)[2:].zfill(32)[32 - start - length: 32 - start]


def remove_reg_alias(line: str) -> str:
    def repl(m: re.Match) -> str:
        name = m.group(1).lower()
        return f"x{reg_alias[name]}"
    return alias.sub(repl, line)


def encode_code(code: str):
    op_type = type(InstMap[code.split()[0]])
    # current_type = op_type(code.split()[0], list(code.replace(",", " ").split()[1:]))
    return op_type.encode(
        InstMap[code.split()[0]],
        list(code.replace(",", " ").split()[1:]),
    )


def encode_file(path: str):
    global pc
    global list_label_name
    global list_label_pc
    pc = 0x00000000
    list_label_name.clear()
    list_label_pc.clear()
    instline = read_file_as_list(path)
    for i in range(len(instline)):
        instline[i] = instline[i].split("#", 1)[0].split("//", 1)[0].strip()
        if not instline[i]:
            instline[i] = ""
            continue
        if ":" in instline[i]:
            label = instline[i].split(":", 1)[0].strip()
            rest = instline[i].split(":", 1)[1].strip()
            list_label_name.append(label)
            list_label_pc.append(pc)
            if rest:
                instline[i] = remove_reg_alias(rest)
                pc += 0x00000004
            else:
                instline[i] = label + ":"
        else:
            instline[i] = remove_reg_alias(instline[i])
            pc += 0x00000004
    pc = 0x00000000
    assemble_code = []
    for i in instline:
        if ":" not in i and i:
            assemble_code.append(encode_code(i))
            pc += 0x00000004
    write_file("./assembler/firmware.hex", assemble_code)
