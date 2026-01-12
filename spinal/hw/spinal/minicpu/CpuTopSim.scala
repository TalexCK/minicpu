package minicpu

import spinal.core._
import spinal.core.sim._

import java.io._
import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object CpuTopSim extends App {
  private val config = RiscvConfig()

  private val firmwarePath =
    sys.env.getOrElse("FIRMWARE", "../assembler/firmware.hex")
  private val commitLogPath =
    sys.env.getOrElse("COMMIT_LOG", "../logs/minicpu.log")

  private val maxCycles = sys.env
    .get("MAX_INSTRUCTIONS")
    .flatMap(s => scala.util.Try(s.toInt).toOption)
    .getOrElse(500)

  private val abiNames = Array(
    "zero",
    "ra",
    "sp",
    "gp",
    "tp",
    "t0",
    "t1",
    "t2",
    "s0",
    "s1",
    "a0",
    "a1",
    "a2",
    "a3",
    "a4",
    "a5",
    "a6",
    "a7",
    "s2",
    "s3",
    "s4",
    "s5",
    "s6",
    "s7",
    "s8",
    "s9",
    "s10",
    "s11",
    "t3",
    "t4",
    "t5",
    "t6"
  )
  private def reg(i: Int): String = abiNames(i & 31)

  private def toUnsigned32(x: scala.math.BigInt): Long =
    java.lang.Integer.toUnsignedLong(x.intValue)

  private def loadFirmwareHex(path: String): Map[Long, Int] = {
    val p = Paths.get(path)
    if (!Files.exists(p)) {
      val cwd = Paths.get("").toAbsolutePath
      throw new FileNotFoundException(s"Firmware not found: $path (cwd=$cwd)")
    }

    val lines = Files.readAllLines(p).asScala.toVector
    val mem = mutable.LinkedHashMap.empty[Long, Int]

    var wordAddr: Long = 0x80000000L

    def parseWordToken(tok: String): Option[Int] = {
      val t = tok.trim
      if (t.isEmpty) None
      else if (t.matches("[01]{32}"))
        Some(java.lang.Long.parseUnsignedLong(t, 2).toInt)
      else if (t.matches("[0-9a-fA-F]{1,8}"))
        Some(java.lang.Long.parseUnsignedLong(t, 16).toInt)
      else None
    }

    for (raw <- lines) {
      val s0 = raw.trim
      val tokens = s0.split("\\s+")
      var any = false
      tokens.foreach { tok =>
        parseWordToken(tok).foreach { w =>
          any = true
          mem.update(wordAddr, w)
          wordAddr += 1
        }
        if (!any && s0.matches("[01]+") && s0.length % 32 == 0) {
          val n = s0.length / 32
          var i = 0
          while (i < n) {
            val chunk = s0.substring(i * 32, (i + 1) * 32)
            val w = java.lang.Long.parseUnsignedLong(chunk, 2).toInt
            mem.update(wordAddr, w)
            wordAddr += 1
            i += 1
          }
        }
      }
    }

    if (mem.isEmpty)
      throw new RuntimeException(s"Parsed 0 words from firmware: $path")
    mem.toMap
  }

  private def preloadMem(dut: CpuTop, words: Map[Long, Int]): Unit = {
    val iDepth = dut.iMem.mem.wordCount
    val dDepth = dut.dMem.mem.wordCount
    if (iDepth <= 0 || dDepth <= 0)
      throw new RuntimeException("Memory depth is not positive")

    words.foreach { case (wa, w) =>
      val idxI = (java.lang.Long.remainderUnsigned(wa, iDepth.toLong)).toInt
      val idxD = (java.lang.Long.remainderUnsigned(wa, dDepth.toLong)).toInt
      val bi = scala.math.BigInt(java.lang.Integer.toUnsignedLong(w))
      dut.iMem.mem.setBigInt(idxI, bi)
      dut.dMem.mem.setBigInt(idxD, bi)
    }
  }

  private val firmwareWords = loadFirmwareHex(firmwarePath)

  SimConfig.withWave
    .compile {
      val dut = new CpuTop(config)

      dut.pc.io.bus.pc.simPublic()
      dut.regFile.regs.simPublic()

      dut.iMem.mem.simPublic()
      dut.dMem.mem.simPublic()

      dut
    }
    .doSim { dut =>
      preloadMem(dut, firmwareWords)

      val outFile = new File(commitLogPath)
      if (outFile.getParentFile != null) outFile.getParentFile.mkdirs()
      val out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))

      dut.clockDomain.forkStimulus(period = 10)
      dut.clockDomain.assertReset()
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.deassertReset()

      var cycle = 0
      var samePcStreak = 0
      var lastPc: Long = -1L

      while (cycle < maxCycles && samePcStreak < 2000) {
        val pc = toUnsigned32(dut.pc.io.bus.pc.toBigInt)
        if (pc == lastPc) samePcStreak += 1 else samePcStreak = 0
        lastPc = pc

        val regsText = (0 until 32)
          .map { r =>
            val v =
              if (r == 0) 0L
              else toUnsigned32(dut.regFile.regs(r).toBigInt)
            f"${reg(r)}=0x$v%08x"
          }
          .mkString(" ")

        out.println(f"--time:no.$cycle%d pc=0x$pc%08x $regsText")

        dut.clockDomain.waitRisingEdge()
        cycle += 1
      }

      out.flush()
      out.close()
    }
}
