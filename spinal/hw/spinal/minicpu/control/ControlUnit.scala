package minicpu.control

import spinal.core._
import spinal.lib.master
import minicpu.RiscvConfig
import minicpu.interfaces.ControlUnitBus
import minicpu.interfaces.AluCtrlOp
import minicpu.interfaces.ImmType

class ControlUnit(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val bus = master(ControlUnitBus(config))
  }

  // Default values
  io.bus.regWriteEnable := False
  io.bus.aluCtrlOp := AluCtrlOp.ADD_TYPE
  io.bus.aluSrc1 := False
  io.bus.aluSrc2 := False
  io.bus.memRead := False
  io.bus.memWrite := False
  io.bus.memToReg := 0
  io.bus.immType := ImmType.I
  io.bus.branch := False
  io.bus.jump := False

  switch(io.bus.opcode) {
    is(U"7'b0110011") {
      // R
      io.bus.regWriteEnable := True
      io.bus.aluCtrlOp := AluCtrlOp.R_TYPE
    }
    is(U"7'b0010011") {
      // I
      io.bus.regWriteEnable := True
      io.bus.aluCtrlOp := AluCtrlOp.I_TYPE
      io.bus.aluSrc2 := True
    }
    is(U"7'b0000011") {
      // Load
      io.bus.regWriteEnable := True
      io.bus.aluSrc2 := True
      io.bus.memRead := True
      io.bus.memToReg := 1
      io.bus.immType := ImmType.I
    }
    is(U"7'b0100011") {
      // Store
      io.bus.aluSrc2 := True
      io.bus.memWrite := True
      io.bus.immType := ImmType.S
    }
    is(U"7'b1100011") {
      // Branch
      io.bus.branch := True
      io.bus.immType := ImmType.B
      io.bus.aluCtrlOp := AluCtrlOp.B_TYPE
    }
    is(U"7'b1101111") {
      // JAL
      io.bus.regWriteEnable := True
      io.bus.aluSrc1 := True
      io.bus.aluSrc2 := True
      io.bus.jump := True
      io.bus.memToReg := 2
      io.bus.immType := ImmType.J
    }
    is(U"7'b1100111") {
      // JALR
      io.bus.regWriteEnable := True
      io.bus.aluSrc2 := True
      io.bus.jump := True
      io.bus.memToReg := 2
      io.bus.immType := ImmType.I
    }
    is(U"7'b0110111") {
      // LUI
      io.bus.regWriteEnable := True
      io.bus.aluSrc2 := True
      io.bus.immType := ImmType.U
      io.bus.aluCtrlOp := AluCtrlOp.COPY_SRC2
    }
    is(U"7'b0010111") {
      // AUIPC
      io.bus.regWriteEnable := True
      io.bus.aluSrc1 := True
      io.bus.aluSrc2 := True
      io.bus.immType := ImmType.U
    }
  }
}
