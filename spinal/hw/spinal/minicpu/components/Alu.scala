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
    }
    is(AluOp.SLTU) {
      io.bus.result := (io.bus.src1 < io.bus.src2).asUInt
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
    is(AluOp.JALR) {
      io.bus.result := (io.bus.src1 + io.bus.src2) & (~U(1))
    }
    is(AluOp.BEQ) {
      io.bus.result := (io.bus.src1 === io.bus.src2).asUInt
    }
    is(AluOp.BNE) {
      io.bus.result := (io.bus.src1 =/= io.bus.src2).asUInt
    }
    is(AluOp.BGE) {
      io.bus.result := (io.bus.src1.asSInt >= io.bus.src2.asSInt).asUInt
    }
    is(AluOp.BGEU) {
      io.bus.result := (io.bus.src1 >= io.bus.src2).asUInt
    }
    is(AluOp.LUI) {
      io.bus.result := io.bus.src2 << U(12)
    }
    is(AluOp.AUIPC) {
      io.bus.result := io.bus.src1 + (io.bus.src2 << U(12))
    }
    default {
      io.bus.result := U(0)
    }
  }
}
