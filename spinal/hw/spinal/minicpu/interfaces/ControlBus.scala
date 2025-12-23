package minicpu.interfaces

import minicpu.RiscvConfig
import spinal.lib.IMasterSlave
import spinal.core._

// define control signals bundle (Controller -> Datapath)
case class ControlSignals(config: RiscvConfig) extends Bundle {
  val regWriteEnable = Bool()
  val aluOp = AluOp()
  val aluSrc1 = Bool() // ALU source 1 (0: register, 1: PC)
  val aluSrc2 = Bool() // ALU source 2 (0: register, 1: immediate)
  val memRead = Bool() // enable memory read
  val memWrite = Bool() // enable memory write
  val memToReg = UInt(
    2 bits
  ) // select source of data (0: ALU result, 1: memory data, 2: PC + 4)
}
