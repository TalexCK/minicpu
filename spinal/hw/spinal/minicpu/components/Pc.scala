package minicpu.components

import minicpu.RiscvConfig
import spinal.core._

class Pc(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val pcNext = in(config.addressType)
    val we = in(Bool())
    val pc = out(config.addressType)
  }

  val initValue = U(config.resetVector, config.addrWidth bits)
  val reg = RegInit(initValue)

  when(io.we) {
    reg := io.pcNext
  }
  io.pc := reg

}
