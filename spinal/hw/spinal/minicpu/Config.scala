package minicpu

import spinal.core._

// Parameters for the RISC-V CPU
case class RiscvConfig(
    xlen: Int = 32, // register width (RV32I)
    addrWidth: Int = 32,
    resetVector: Long = 0x00000000l // PC reset value
) {
  // generate UInt types based on xlen and addrWidth
  def wordType = UInt(xlen bits)
  def addressType = UInt(addrWidth bits)
}

object ProjectConfig {
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    )
  )
}
