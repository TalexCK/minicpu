package minicpu.components

import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.slave
import minicpu.interfaces.{RegFileRead, RegFileWrite}

class RegFile(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val rs1 = slave(RegFileRead(config))
    val rs2 = slave(RegFileRead(config))

    val rd = slave(RegFileWrite(config))
  }

  val regs = Mem(config.wordType, 32)

  io.rs1.data := Mux(
    io.rs1.address === 0,
    U(0, config.xlen bits),
    regs.readAsync(io.rs1.address)
  )

  io.rs2.data := Mux(
    io.rs2.address === 0,
    U(0, config.xlen bits),
    regs.readAsync(io.rs2.address)
  )

  when(io.rd.we && io.rd.address =/= 0) {
    regs.write(io.rd.address, io.rd.data)
  }

}
