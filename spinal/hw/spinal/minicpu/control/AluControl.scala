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
    is(AluCtrlOp.SUB_TYPE) {
      io.bus.aluOp := AluOp.SUB
    }
    is(AluCtrlOp.R_TYPE, AluCtrlOp.I_TYPE) {
      switch(io.bus.funct3) {
        is(B"000") {
          when(io.bus.aluCtrlOp === AluCtrlOp.R_TYPE && io.bus.funct7(5)) {
            io.bus.aluOp := AluOp.SUB
          } otherwise {
            io.bus.aluOp := AluOp.ADD
          }
        }
        is(B"001") {
          io.bus.aluOp := AluOp.SLL
        }
        is(B"010") {
          io.bus.aluOp := AluOp.SLT
        }
        is(B"011") {
          io.bus.aluOp := AluOp.SLTU
        }
        is(B"100") {
          io.bus.aluOp := AluOp.XOR
        }
        is(B"101") {
          when(io.bus.funct7(5)) {
            io.bus.aluOp := AluOp.SRA
          } otherwise {
            io.bus.aluOp := AluOp.SRL
          }
        }
        is(B"110") {
          io.bus.aluOp := AluOp.OR
        }
        is(B"111") {
          io.bus.aluOp := AluOp.AND
        }
      }
    }
    is(AluCtrlOp.COPY_SRC2) {
      io.bus.aluOp := AluOp.COPY_SRC2
    }
  }
}
