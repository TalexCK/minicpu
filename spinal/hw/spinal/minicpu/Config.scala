package minicpu

import spinal.core._

// Parameters for the RISC-V CPU
case class RiscvConfig(
    xlen: Int = 32, // register width (RV32I)
    addrWidth: Int = 32,
    resetVector: Long = 0x80000000L, // PC reset value
    memorySize: Long = 1024 * 64, // 64 KB memory
    regAddrWidth: Int = 5 // 5 bits register address
) {
  def wordType = UInt(xlen bits)
  def addressType = UInt(addrWidth bits)
  def regAddrType = UInt(regAddrWidth bits)
}

object ProjectConfig {
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    )
  )
}
