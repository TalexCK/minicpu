package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.lib.IMasterSlave
import spinal.core._

object AluCtrlOp extends SpinalEnum {
  val ADD_TYPE, SUB_TYPE, R_TYPE, I_TYPE, COPY_SRC2 = newElement()
}

case class AluControlBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val aluCtrlOp = AluCtrlOp()
  val funct3 = UInt(3 bits)
  val funct7 = UInt(7 bits)
  val aluOp = AluOp()

  override def asMaster(): Unit = {
    out(aluCtrlOp, funct3, funct7)
    in(aluOp)
  }
}

case class ControlUnitBus(config: RiscvConfig)
    extends Bundle
    with IMasterSlave {
  val opcode = UInt(7 bits)
  val regWriteEnable = Bool()
  val aluCtrlOp = AluCtrlOp()
  val aluSrc1 = Bool() // 0: register, 1: PC
  val aluSrc2 = Bool() // 0: register, 1: immediate
  val memRead = Bool()
  val memWrite = Bool()
  val memToReg = UInt(
    2 bits
  ) // 0: ALU result, 1: memory data, 2: PC + 4
  val immType = ImmType()
  val branch = Bool()
  val jump = Bool()

  override def asMaster(): Unit = {
    in(
      opcode
    )
    out(
      regWriteEnable,
      aluCtrlOp,
      aluSrc1,
      aluSrc2,
      memRead,
      memWrite,
      memToReg,
      immType,
      branch,
      jump
    )
  }
}
