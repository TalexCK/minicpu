package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.core.{Bundle, SpinalEnum, in, out}
import spinal.lib.IMasterSlave

object ImmType extends SpinalEnum {
  val I, U, J, B, S = newElement()
}

case class ImmGenBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val instruction = config.instructionType
  val immSel = config.immSelType
  val imm = config.wordType

  override def asMaster(): Unit = {
    out(imm)
    in(instruction, immSel)
  }
}