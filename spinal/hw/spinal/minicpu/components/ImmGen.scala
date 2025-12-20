package minicpu.components

import minicpu.interfaces.ImmType
import minicpu.RiscvConfig
import spinal.core._

class ImmGen(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val instruction = in(config.instructionType)
    val immSel = in(config.immSelType)
    val imm = out(config.wordType)
  }

  def resizeImm(src: Bits): UInt = src.asSInt.resize(config.xlen).asUInt
  val inst = io.instruction

  switch(io.immSel) {
    is(ImmType.typeI) {
      io.imm := resizeImm(inst(31 downto 20))
    }
    is(ImmType.typeS) {
      io.imm := resizeImm(inst(31 downto 25) ## inst(11 downto 7))
    }
    is(ImmType.typeB) {
      io.imm := resizeImm(inst(31) ## inst(7) ## inst(30 downto 25) ## inst(11 downto 8) ## B(0, 1 bits))
    }
    is(ImmType.typeU) {
      io.imm := resizeImm(inst(31 downto 12) ## B(0, 12 bits))
    }
    is(ImmType.typeJ) {
      io.imm := resizeImm(inst(31) ## inst(19 downto 12) ## inst(20) ## inst(30 downto 21) ## B(0, 1 bits))
    }
    default {
      io.imm := U(0, config.xlen bits)
    }
  }
}
