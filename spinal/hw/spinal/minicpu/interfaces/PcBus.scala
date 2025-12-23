package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.IMasterSlave

case class PcBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val pcNext = config.addressType
  val we = Bool()
  val pc = config.addressType

  override def asMaster(): Unit = {
    out(pcNext, we)
    in(pc)
  }
}
