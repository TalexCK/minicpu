package minicpu

import spinal.core._
import minicpu.components._
import minicpu.interfaces._
import minicpu.control._

class CpuTop(config: RiscvConfig) extends Component {
  val pc = new Pc(config)
  val control = new ControlUnit(config)
  val regFile = new RegFile(config)
  val immGen = new ImmGen(config)
  val alu = new Alu(config)
  val aluCtrl = new AluControl(config)

  val iMem = new Memory(config)
  val dMem = new Memory(config)

  // Decoding
  val currentPc = pc.io.bus.pc

  iMem.io.bus.enable := True
  iMem.io.bus.address := currentPc
  iMem.io.bus.write := False
  iMem.io.bus.writeData := 0

  val instruction = iMem.io.bus.readData

  val opcode = instruction(6 downto 0)
  val rd = instruction(11 downto 7)
  val funct3 = instruction(14 downto 12)
  val rs1 = instruction(19 downto 15)
  val rs2 = instruction(24 downto 20)
  val funct7 = instruction(31 downto 25)

  control.io.bus.opcode := opcode
  regFile.io.rs1.address := rs1
  regFile.io.rs2.address := rs2

  immGen.io.bus.instruction := instruction
  immGen.io.bus.immSel := control.io.bus.immType

  aluCtrl.io.bus.funct3 := funct3
  aluCtrl.io.bus.funct7 := funct7
  aluCtrl.io.bus.aluCtrlOp := control.io.bus.aluCtrlOp

  // Execution
  val aluSrc1 = Mux(control.io.bus.aluSrc1, currentPc, regFile.io.rs1.data)
  val aluSrc2 =
    Mux(control.io.bus.aluSrc2, immGen.io.bus.imm, regFile.io.rs2.data)

  alu.io.bus.op := aluCtrl.io.bus.aluOp
  alu.io.bus.src1 := aluSrc1
  alu.io.bus.src2 := aluSrc2

  // Memory Access
  dMem.io.bus.enable := control.io.bus.memRead || control.io.bus.memWrite
  dMem.io.bus.write := control.io.bus.memWrite
  dMem.io.bus.address := alu.io.bus.result
  dMem.io.bus.writeData := regFile.io.rs2.data

  // Write Back
  val writeBackData = UInt(config.xlen bits)
  switch(control.io.bus.memToReg) {
    is(0) {
      writeBackData := alu.io.bus.result
    }
    is(1) {
      writeBackData := dMem.io.bus.readData
    }
    is(2) {
      writeBackData := currentPc + 4
    }
    default {
      writeBackData := 0
    }
  }

  regFile.io.rd.address := rd
  regFile.io.rd.data := writeBackData
  regFile.io.rd.we := control.io.bus.regWriteEnable

  // PC Update
  val nextPc = UInt(config.addrWidth bits)
  nextPc := currentPc + 4

  val branchTarget = currentPc + immGen.io.bus.imm
  val jalrTarget =
    (alu.io.bus.result.resize(config.addrWidth) & ~U(1, config.addrWidth bits))

  val branchTaken = False
  when(control.io.bus.branch) {
    switch(funct3) {
      is(U"3'b000") { // BEQ
        branchTaken := alu.io.bus.zero
      }
      is(U"3'b001") { // BNE
        branchTaken := !alu.io.bus.zero
      }
      is(U"3'b100") { // BLT
        branchTaken := alu.io.bus.result.asBits.msb
      }
      is(U"3'b101") { // BGE
        branchTaken := !alu.io.bus.result.asBits.msb
      }
      is(U"3'b110") { // BLTU
        branchTaken := alu.io.bus.result(0)
      }
      is(U"3'b111") { // BGEU
        branchTaken := !alu.io.bus.result(0)
      }
    }
  }

  when(control.io.bus.jump) {
    when(opcode === U"7'b1100111") {
      // JALR
      nextPc := jalrTarget
    } otherwise {
      // JAL
      nextPc := branchTarget
    }
  } elsewhen (branchTaken) {
    nextPc := branchTarget
  }

  pc.io.bus.pcNext := nextPc
  pc.io.bus.we := True
}
