package minicpu.components

import minicpu.RiscvConfig
import spinal.core._
import minicpu.interfaces.AluOp

class Alu(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val op = in(AluOp())
    val src1 = in(config.wordType)
    val src2 = in(config.wordType)
    val result = out(config.wordType)
  }

  switch(io.op) {
    is(AluOp.ADD) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.SUB) {
      io.result := io.src1 - io.src2
    }
    is(AluOp.SLL) {
      val shamt = io.src2(4 downto 0)
      io.result := io.src1 |<< shamt
    }
    is(AluOp.SLT) {
      io.result := (io.src1.asSInt < io.src2.asSInt).asUInt
    }
    is(AluOp.SLTU) {
      io.result := (io.src1 < io.src2).asUInt
    }
    is(AluOp.XOR) {
      io.result := io.src1 ^ io.src2
    }
    is(AluOp.SRL) {
      val shamt = io.src2(4 downto 0)
      io.result := io.src1 |>> shamt
    }
    is(AluOp.SRA) {
      val shamt = io.src2(4 downto 0)
      io.result := (io.src1.asSInt >> shamt).asUInt
    }
    is(AluOp.OR) {
      io.result := io.src1 | io.src2
    }
    is(AluOp.AND) {
      io.result := io.src1 & io.src2
    }
    is(AluOp.ADDI) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.SLTI) {
      io.result := (io.src1.asSInt < io.src2.asSInt).asUInt
    }
    is(AluOp.SLTIU) {
      io.result := (io.src1 < io.src2).asUInt
    }
    is(AluOp.XORI) {
      io.result := io.src1 ^ io.src2
    }
    is(AluOp.ORI) {
      io.result := io.src1 | io.src2
    }
    is(AluOp.ANDI) {
      io.result := io.src1 & io.src2
    }
    is(AluOp.SLLI) {
      val shamt = io.src2(4 downto 0)
      io.result := io.src1 |<< shamt
    }
    is(AluOp.SRLI) {
      val shamt = io.src2(4 downto 0)
      io.result := io.src1 |>> shamt
    }
    is(AluOp.SRAI) {
      val shamt = io.src2(4 downto 0)
      io.result := (io.src1.asSInt >> shamt).asUInt
    }
    is(AluOp.LB) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.LH) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.LW) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.LBU) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.LHU) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.JALR) {
      io.result := (io.src1 + io.src2) & (~U(1))
    }
    is(AluOp.ECALL) {
      io.result := U(0)
    }
    is(AluOp.EBREAK) {
      io.result := U(0)
    }
    is(AluOp.SB) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.SH) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.SW) {
      io.result := io.src1 + io.src2
    }
    is(AluOp.BEQ) {
      io.result := (io.src1 === io.src2).asUInt
    }
    is(AluOp.BNE) {
      io.result := (io.src1 =/= io.src2).asUInt
    }
    is(AluOp.BLT) {
      io.result := (io.src1.asSInt < io.src2.asSInt).asUInt
    }
    is(AluOp.BGE) {
      io.result := (io.src1.asSInt >= io.src2.asSInt).asUInt
    }
    is(AluOp.BLTU) {
      io.result := (io.src1 < io.src2).asUInt
    }
    is(AluOp.BGEU) {
      io.result := (io.src1 >= io.src2).asUInt
    }
    is(AluOp.LUI) {
      io.result := io.src2 << U(12)
    }
    is(AluOp.AUIPC) {
      io.result := io.src1 + (io.src2 << U(12))
    }
    is(AluOp.JAL) {
      io.result := io.src1 + io.src2
    }
    default {
      io.result := U(0)
    }
  }
}
