package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.core.{Bundle, SpinalEnum, in, out}
import spinal.lib.IMasterSlave

object ImmType extends SpinalEnum {
  val I, U, J, B, S = newElement()
}

case class ImmGenBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val instruction = config.wordType
  val immSel = ImmType()
  val imm = config.wordType

  override def asMaster(): Unit = {
    in(imm)
    out(instruction, immSel)
  }
}
