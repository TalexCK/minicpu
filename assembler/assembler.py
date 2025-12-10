from dataclasses import dataclass
from typing import Type


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
        rd = int(args[0][1:])
        rs1 = int(args[1][1:])
        rs2 = int(args[2][1:])
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

    def encode(self, args: list):
        opcode = self.opcode
        rd = int(args[0][1:])
        rs1 = int(args[1][1:])
        funct3 = self.funct3
        imm = int(args[2])
        # funct7_is_imm = self.funct7_is_imm
        # if funct7_is_imm:
        #     funct7_imm = self.imm
        #     funct7_imm_start = self.imm_start
        pass


class SType(AssemblerType):
    opcode: int
    funct3: int

    def encode(self, args: list):
        opcode = self.opcode
        funct3 = self.funct3
        rs1 = int(args[0][1:])
        rs2 = int(args[1][1:])
        imm = int(args[2])
        pass


class BType(AssemblerType):
    opcode: int
    funct3: int

    def encode(self, args: list):
        opcode = self.opcode
        funct3 = self.funct3
        rs1 = int(args[0][1:])
        rs2 = int(args[1][1:])
        imm = int(args[2])
        pass


class UType(AssemblerType):
    opcode: int

    def encode(self, args: list):
        opcode = self.opcode
        rd = int(args[0][1:])
        imm = int(args[1])
        pass


class JType(AssemblerType):
    opcode: int

    def encode(self, args: list):
        opcode = self.opcode
        rd = int(args[0][1:])
        imm = int(args[1])
        pass


Inst_Map = {
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
    "slli": IType(opcode=0x13, funct3=0x1, imm=0x00, imm_start=0x5),
    "srli": IType(opcode=0x13, funct3=0x5, imm=0x00, imm_start=0x5),
    "srai": IType(opcode=0x13, funct3=0x5, imm=0x20, imm_start=0x5),
    "lb": IType(opcode=0x03, funct3=0x0),
    "lh": IType(opcode=0x03, funct3=0x1),
    "lw": IType(opcode=0x03, funct3=0x2),
    "lbu": IType(opcode=0x03, funct3=0x4),
    "lhu": IType(opcode=0x03, funct3=0x5),
    "jalr": IType(opcode=0x67, funct3=0x0),
    "ecall": IType(opcode=0x73, funct3=0x0, imm=0x0),
    "ebreak": IType(opcode=0x73, funct3=0x0, imm=0x1),
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
def to_bin(num: int, length: int):
    return bin(num)[2:].zfill(length)


def encode_code(code: str):
    opType = type(Inst_Map[code.split()[0]])
    # current_type = op_type(code.split()[0], list(code.replace(",", " ").split()[1:]))
    return opType.encode(
        Inst_Map[code.split()[0]], list(code.replace(",", " ").split()[1:])
    )
