package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.lib.IMasterSlave
import spinal.core._

// define a bundle
case class SimpleBus(config: RiscvConfig) extends Bundle with IMasterSlave {
  val enable = Bool() // indicates a valid transaction
  val write = Bool() // indicates read (0) or write (1)
  val address = config.addressType // address for the transaction
  val writeData = config.wordType // data to write (from CPU to memory)
  val readData = config.wordType // data read (from memory to CPU)

  // define the direction of master(CPU)
  override def asMaster(): Unit = {
    out(enable, write, address, writeData)
    in(readData)
  }
}

// define ALU operations
object AluOp extends SpinalEnum {
  val ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND, ADDI, SLTI, SLTIU, XORI,
      ORI, ANDI, SLLI, SRLI, SRAI, LB, LH, LW, LBU, LHU, JALR, ECALL, EBREAK,
      SB, SH, SW, BEQ, BNE, BLT, BGE, BLTU, BGEU, LUI, AUIPC, JAL =
    newElement();
}

// define control signals bundle(Controller -> Datapath)
case class ControlSignals(config: RiscvConfig) extends Bundle {
  val regWriteEnable = Bool() // enable register write
  val aluOp = AluOp() // ALU operation
  val aluSrc1 = Bool() // ALU source 1 (0: register, 1: PC)
  val aluSrc2 = Bool() // ALU source 2 (0: register, 1: immediate)
  val memRead = Bool() // enable memory read
  val memWrite = Bool() // enable memory write
  val memToReg = UInt(
    2 bits
  ) // select source of data (0: ALU result, 1: memory data, 2: PC + 4)
}
