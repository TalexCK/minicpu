package minicpu

object CpuTopVerilog {
  def main(args: Array[String]): Unit = {
    val cpuConfig = RiscvConfig()

    ProjectConfig.spinal.generateVerilog(new CpuTop(cpuConfig))

    println("Done.")
  }
}
