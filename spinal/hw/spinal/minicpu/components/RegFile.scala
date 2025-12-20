package minicpu.components

import minicpu.RiscvConfig
import spinal.core._

class RegFile(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val rs1Addr = in(config.regAddrType)
    val rs2Addr = in(config.regAddrType)
    val rdAddr  = in(config.regAddrType)
    val rdData  = in(config.dataBitsType)
    val we      = in(Bool())
    val rs1Data = out(config.dataBitsType)
    val rs2Data = out(config.dataBitsType)
  }

  val regs = Mem(config.dataBitsType, 32)

  io.rs1Data := Mux(
    io.rs1Addr === 0,
    B(0, config.xlen bits),
    regs.readAsync(io.rs1Addr)
  )
  io.rs2Data := Mux(
    io.rs2Addr === 0,
    B(0, config.xlen bits),
    regs.readAsync(io.rs2Addr)
  )

  when(io.we && io.rdAddr =/= 0) {
    regs.write(io.rdAddr, io.rdData)
  }


}