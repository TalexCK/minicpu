package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.IMasterSlave

case class MemoryBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val enable = Bool()
  val write = Bool() // read (0) or write (1)
  val address = config.addressType
  val writeData = config.wordType
  val mask = Bits(config.xlen / 8 bits)
  val readData = config.wordType

  override def asMaster(): Unit = {
    out(enable, write, address, writeData, mask)
    in(readData)
  }
}
