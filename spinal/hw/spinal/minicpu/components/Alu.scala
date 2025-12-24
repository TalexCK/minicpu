package minicpu.components

import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.slave
import minicpu.interfaces.AluBus
import minicpu.interfaces.AluOp

class Alu(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val bus = slave(AluBus(config))
  }

  io.bus.result := U(0, config.xlen bits)

  switch(io.bus.op) {
    is(AluOp.ADD) {
      io.bus.result := io.bus.src1 + io.bus.src2
    }
    is(AluOp.SUB) {
      io.bus.result := io.bus.src1 - io.bus.src2
    }
    is(AluOp.SLL) {
      val shamt = io.bus.src2(4 downto 0)
      io.bus.result := io.bus.src1 |<< shamt
    }
    is(AluOp.SLT) {
      io.bus.result := (io.bus.src1.asSInt < io.bus.src2.asSInt).asUInt
        .resize(config.xlen)
    }
    is(AluOp.SLTU) {
      io.bus.result := (io.bus.src1 < io.bus.src2).asUInt.resize(config.xlen)
    }
    is(AluOp.XOR) {
      io.bus.result := io.bus.src1 ^ io.bus.src2
    }
    is(AluOp.SRL) {
      val shamt = io.bus.src2(4 downto 0)
      io.bus.result := io.bus.src1 |>> shamt
    }
    is(AluOp.SRA) {
      val shamt = io.bus.src2(4 downto 0)
      io.bus.result := (io.bus.src1.asSInt >> shamt).asUInt
    }
    is(AluOp.OR) {
      io.bus.result := io.bus.src1 | io.bus.src2
    }
    is(AluOp.AND) {
      io.bus.result := io.bus.src1 & io.bus.src2
    }
    is(AluOp.COPY_SRC2) {
      io.bus.result := io.bus.src2
    }
  }

  io.bus.zero := (io.bus.result === 0)
}
