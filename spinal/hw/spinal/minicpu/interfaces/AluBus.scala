package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.IMasterSlave

object AluOp extends SpinalEnum {
  val ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND, ADDI, SLTI, SLTIU, XORI,
      ORI, ANDI, SLLI, SRLI, SRAI, LB, LH, LW, LBU, LHU, JALR, ECALL, EBREAK,
      SB, SH, SW, BEQ, BNE, BLT, BGE, BLTU, BGEU, LUI, AUIPC, JAL =
    newElement();
}

case class AluBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val op = AluOp()
  val src1 = config.wordType
  val src2 = config.wordType
  val result = config.wordType

  override def asMaster(): Unit = {
    out(op, src1, src2)
    in(result)
  }
}
