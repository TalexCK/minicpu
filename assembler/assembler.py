from utils import read_file_as_list, write_file


class AssemblerType:
    def __init__(self, **kwargs):
        for key, value in kwargs.items():
            setattr(self, key, value)


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
        if opcode == 0x41:
            rs1 = 0
        else:
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
        if opcode == 0x41:
            return "00000000000000000000000000010011"
        elif opcode == 0x03 or opcode == 0x67:
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
            if opcode == 0x42:
                opcode = 0x13
                imm = 0
            elif opcode == 0x43:
                opcode = 0x13
                imm = -1
            else:
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
        imm = int(args[2], 0)

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
        imm = int(args[1], 0)

        return to_bin(imm, 20) + to_bin(rd, 5) + to_bin(opcode, 7)


class JType(AssemblerType):
    opcode: int

    def encode(self, args: list):
        opcode = self.opcode
        rd = int(args[0][1:], 0)
        imm = int(args[1], 0)

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
    "neg": RType(opcode=0x41, funct3=0x0, funct7=0x20),
    "sll": RType(opcode=0x33, funct3=0x1, funct7=0x00),
    "slt": RType(opcode=0x33, funct3=0x2, funct7=0x00),
    "sltu": RType(opcode=0x33, funct3=0x3, funct7=0x00),
    "xor": RType(opcode=0x33, funct3=0x4, funct7=0x00),
    "srl": RType(opcode=0x33, funct3=0x5, funct7=0x00),
    "sra": RType(opcode=0x33, funct3=0x5, funct7=0x20),
    "or": RType(opcode=0x33, funct3=0x6, funct7=0x00),
    "and": RType(opcode=0x33, funct3=0x7, funct7=0x00),
    "addi": IType(opcode=0x13, funct3=0x0),
    "not": IType(opcode=0x43, funct3=0x4),
    "mv": IType(opcode=0x42, funct3=0x0),
    "nop": IType(opcode=0x41, funct3=0x0),
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


# 转化为二进制并确保长度
def to_bin(num: int, length: int, start: int = 0):
    if num < 0:
        return (bin(num & 0xFFFFFFFF)[2:].zfill(32))[32 - start - length : 32 - start]
    return bin(num)[2:].zfill(32)[32 - start - length : 32 - start]


def encode_code(code: str):
    op_type = type(InstMap[code.split()[0]])
    # current_type = op_type(code.split()[0], list(code.replace(",", " ").split()[1:]))
    return op_type.encode(
        InstMap[code.split()[0]],
        list(code.replace("sp", "x2").replace(",", " ").split()[1:]),
    )


def encode_file(path: str):
    assemble_code = []
    if_start = False
    for i in read_file_as_list(path):
        if if_start:
            if i[0] == ".":
                if_start = False
            elif i.split()[0] == "#":
                continue
            else:
                assemble_code.append(encode_code(i))
        elif i == "_start:":
            if_start = True

    write_file("./code.hex", assemble_code)
