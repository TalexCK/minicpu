package minicpu.components

import minicpu.interfaces.{ImmGenBus, ImmType}
import minicpu.RiscvConfig
import spinal.core._
import spinal.lib.slave

class ImmGen(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val bus = slave(ImmGenBus(config))
  }

  def resizeImm(src: Bits): UInt = src.asSInt.resize(config.xlen).asUInt
  val inst = io.bus.instruction.asBits

  switch(io.bus.immSel) {
    is(ImmType.I) {
      io.bus.imm := resizeImm(inst(31 downto 20))
    }
    is(ImmType.S) {
      io.bus.imm := resizeImm(inst(31 downto 25) ## inst(11 downto 7))
    }
    is(ImmType.B) {
      io.bus.imm := resizeImm(
        inst(31) ## inst(7) ## inst(30 downto 25) ## inst(11 downto 8) ## B(
          0,
          1 bits
        )
      )
    }
    is(ImmType.U) {
      io.bus.imm := resizeImm(inst(31 downto 12) ## B(0, 12 bits))
    }
    is(ImmType.J) {
      io.bus.imm := resizeImm(
        inst(31) ## inst(19 downto 12) ## inst(20) ## inst(30 downto 21) ## B(
          0,
          1 bits
        )
      )
    }
    default {
      io.bus.imm := U(0, config.xlen bits)
    }
  }
}
