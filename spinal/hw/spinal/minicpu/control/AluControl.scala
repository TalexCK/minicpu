package minicpu.control

import spinal.core._
import spinal.lib.slave
import minicpu.interfaces.AluControlBus
import minicpu.RiscvConfig
import minicpu.interfaces.AluOp
import minicpu.interfaces.AluCtrlOp

class AluControl(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val bus = slave(AluControlBus(config))
  }

  io.bus.aluOp := AluOp.ADD

  switch(io.bus.aluCtrlOp) {
    is(AluCtrlOp.ADD_TYPE) {
      io.bus.aluOp := AluOp.ADD
    }
    is(AluCtrlOp.R_TYPE, AluCtrlOp.I_TYPE) {
      switch(io.bus.funct3) {
        is(U"3'b000") {
          when(io.bus.aluCtrlOp === AluCtrlOp.R_TYPE && io.bus.funct7(5)) {
            io.bus.aluOp := AluOp.SUB
          } otherwise {
            io.bus.aluOp := AluOp.ADD
          }
        }
        is(U"3'b001") {
          io.bus.aluOp := AluOp.SLL
        }
        is(U"3'b010") {
          io.bus.aluOp := AluOp.SLT
        }
        is(U"3'b011") {
          io.bus.aluOp := AluOp.SLTU
        }
        is(U"3'b100") {
          io.bus.aluOp := AluOp.XOR
        }
        is(U"3'b101") {
          when(io.bus.funct7(5)) {
            io.bus.aluOp := AluOp.SRA
          } otherwise {
            io.bus.aluOp := AluOp.SRL
          }
        }
        is(U"3'b110") {
          io.bus.aluOp := AluOp.OR
        }
        is(U"3'b111") {
          io.bus.aluOp := AluOp.AND
        }
      }
    }
    is(AluCtrlOp.COPY_SRC2) {
      io.bus.aluOp := AluOp.COPY_SRC2
    }
    is(AluCtrlOp.B_TYPE) {
      switch(io.bus.funct3) {
        is(U"3'b000", U"3'b001") {
          io.bus.aluOp := AluOp.SUB
        }
        is(U"3'b100", U"3'b101") {
          io.bus.aluOp := AluOp.SLT
        }
        is(U"3'b110", U"3'b111") {
          io.bus.aluOp := AluOp.SLTU
        }
      }
    }
  }
}
