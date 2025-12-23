package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.IMasterSlave

object AluOp extends SpinalEnum {
  val ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND = newElement()
  val COPY_SRC2 = newElement()
}

case class AluBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val op = AluOp()
  val src1 = config.wordType
  val src2 = config.wordType
  val result = config.wordType
  val zero = Bool()

  override def asMaster(): Unit = {
    out(op, src1, src2)
    in(result, zero)
  }
}
