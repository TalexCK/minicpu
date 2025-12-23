package minicpu.components

import minicpu.RiscvConfig
import spinal.core._
import minicpu.interfaces.PcBus
import spinal.lib.slave

class Pc(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val bus = slave(PcBus(config))
  }

  val initValue = U(config.resetVector, config.addrWidth bits)
  val reg = RegInit(initValue)

  when(io.bus.we) {
    reg := io.bus.pcNext
  }
  io.bus.pc := reg

}
