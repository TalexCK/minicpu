package minicpu.components

import minicpu.RiscvConfig
import spinal.core._
import minicpu.interfaces.MemoryBus
import spinal.lib.slave

class Memory(config: RiscvConfig) extends Component {
  val io = new Bundle {
    val bus = slave(MemoryBus(config))
  }

  val byteCount = config.memorySize
  val wordCount = byteCount / (config.xlen / 8) // 32 bits width(xlen)

  val mem = Mem(config.wordType, wordCount)

  val wordAddress = (io.bus.address >> 2).resize(mem.addressWidth)

  mem.write(
    enable = io.bus.enable && io.bus.write,
    address = wordAddress,
    data = io.bus.writeData
  )

  io.bus.readData := mem.readAsync(wordAddress)
}
