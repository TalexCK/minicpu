from dataclasses import dataclass
from typing import Type


class AssemblerType:
    def __init__(self, inst:str, args:list):
        self.operation = Inst_Map[inst]
        self.args = args

    def encode(self) -> int:
        pass
# Types
class RType(AssemblerType):
    def encode(self):
        opcode = self.operation.opcode
        funct3 = self.operation.funct3
        funct7 = self.operation.funct7
        rd = int(self.args[0][1:])
        rs1 = int(self.args[1][1:])
        rs2 = int(self.args[2][1:])
        return to_bin(funct7,7)+to_bin(rs2,5)+to_bin(rs1,5)+to_bin(funct3,3)+to_bin(rd,5)+to_bin(opcode,7)


class IType(AssemblerType):
    def encode(self):
        opcode = self.operation.opcode
        rd = int(self.args[0][1:])
        rs1 = int(self.args[1][1:])
        funct3 = self.operation.funct3
        imm = int(self.args[2])
        funct7_is_imm = self.operation.funct7_is_imm
        if funct7_is_imm:
            funct7_imm = self.operation.imm
            funct7_imm_start = self.operation.imm_start
        return 0

class SType(AssemblerType):
    def encode(self):
        opcode = self.operation.opcode
        funct3 = self.operation.funct3
        rs1 = int(self.args[0][1:])
        rs2 = int(self.args[1][1:])
        imm = int(self.args[2])
        return 0

class BType(AssemblerType):
    def encode(self):
        opcode = self.operation.opcode
        funct3 = self.operation.funct3
        rs1 = int(self.args[0][1:])
        rs2 = int(self.args[1][1:])
        imm = int(self.args[2])
        return 0

class UType(AssemblerType):
    def encode(self):
        opcode = self.operation.opcode
        rd = int(self.args[0][1:])
        imm = int(self.args[1])
        return 0

class JType(AssemblerType):
    def encode(self):
        opcode = self.operation.opcode
        rd = int(self.args[0][1:])
        imm = int(self.args[1])
        return 0

# Inst => type,funct3,funct7,opcode的映射
@dataclass
class InstMap:
    type: Type[AssemblerType]
    opcode: int
    funct3: int = None
    funct7: int = None
    imm: int = None
    imm_start: int = 0x0
    funct7_is_imm: bool = False


Inst_Map = {
    "add":  InstMap(RType, 0x33, funct3=0x0, funct7=0x00),
    "sub":  InstMap(RType, 0x33, funct3=0x0, funct7=0x20),
    "sll":  InstMap(RType, 0x33, funct3=0x1, funct7=0x00),
    "slt":  InstMap(RType, 0x33, funct3=0x2, funct7=0x00),
    "sltu": InstMap(RType, 0x33, funct3=0x3, funct7=0x00),
    "xor":  InstMap(RType, 0x33, funct3=0x4, funct7=0x00),
    "srl":  InstMap(RType, 0x33, funct3=0x5, funct7=0x00),
    "sra":  InstMap(RType, 0x33, funct3=0x5, funct7=0x20),
    "or":   InstMap(RType, 0x33, funct3=0x6, funct7=0x00),
    "and":  InstMap(RType, 0x33, funct3=0x7, funct7=0x00),
    "addi":  InstMap(IType, 0x13, funct3=0x0),
    "slti":  InstMap(IType, 0x13, funct3=0x2),
    "sltiu": InstMap(IType, 0x13, funct3=0x3),
    "xori":  InstMap(IType, 0x13, funct3=0x4),
    "ori":   InstMap(IType, 0x13, funct3=0x6),
    "andi":  InstMap(IType, 0x13, funct3=0x7),
    "slli":  InstMap(IType, 0x13, funct3=0x1, imm=0x00, imm_start=0x5, funct7_is_imm=True),
    "srli":  InstMap(IType, 0x13, funct3=0x5, imm=0x00, imm_start=0x5, funct7_is_imm=True),
    "srai":  InstMap(IType, 0x13, funct3=0x5, imm=0x20, imm_start=0x5, funct7_is_imm=True),
    "lb":  InstMap(IType, 0x03, funct3=0x0),
    "lh":  InstMap(IType, 0x03, funct3=0x1),
    "lw":  InstMap(IType, 0x03, funct3=0x2),
    "lbu": InstMap(IType, 0x03, funct3=0x4),
    "lhu": InstMap(IType, 0x03, funct3=0x5),
    "jalr": InstMap(IType, 0x67, funct3=0x0),
    "ecall": InstMap(IType, 0x73, funct3=0x0, imm=0x0, funct7_is_imm=True),
    "ebreak": InstMap(IType, 0x73, funct3=0x0, imm=0x1, funct7_is_imm=True),
    "sb": InstMap(SType, 0x23, funct3=0x0),
    "sh": InstMap(SType, 0x23, funct3=0x1),
    "sw": InstMap(SType, 0x23, funct3=0x2),
    "beq":  InstMap(BType, 0x63, funct3=0x0),
    "bne":  InstMap(BType, 0x63, funct3=0x1),
    "blt":  InstMap(BType, 0x63, funct3=0x4),
    "bge":  InstMap(BType, 0x63, funct3=0x5),
    "bltu": InstMap(BType, 0x63, funct3=0x6),
    "bgeu": InstMap(BType, 0x63, funct3=0x7),
    "lui":   InstMap(UType, 0x37),
    "auipc": InstMap(UType, 0x17),
    "jal": InstMap(JType, 0x6F),
}

# 转化为二进制并确保长度
def to_bin(num:int, length:int):
    return bin(num)[2:].zfill(length)

def encode_code(code:str):
    op_type = Inst_Map[code.split()[0]].type
    current_type = op_type(code.split()[0], list(code.replace(",", " ").split()[1:]))
    return current_type.encode()