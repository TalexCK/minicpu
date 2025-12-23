package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.lib.IMasterSlave
import spinal.core._

case class RegFileWrite(config: RiscvConfig) extends Bundle with IMasterSlave {
  val address = config.regAddrType
  val data = config.wordType
  val we = Bool()

  override def asMaster(): Unit = {
    out(address, data, we)
  }
}

case class RegFileRead(config: RiscvConfig) extends Bundle with IMasterSlave {
  val address = config.regAddrType
  val data = config.wordType

  override def asMaster(): Unit = {
    out(address)
    in(data)
  }
}
