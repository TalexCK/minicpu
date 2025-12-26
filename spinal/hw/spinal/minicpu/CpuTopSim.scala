package minicpu

import spinal.core._
import spinal.core.sim._

import java.io._
import java.nio.file.{Files, Paths}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object CpuTopSim extends App {
  private val config = RiscvConfig()

  private val firmwarePath = sys.props.getOrElse("firmware", "../assembler/firmware.hex")
  private val commitLogPath = sys.props.getOrElse("commitLog", "../logs/minicpu.log")
  private val maxInstructions = sys.props
    .get("maxInstructions")
    .flatMap(s => scala.util.Try(s.toInt).toOption)
    .getOrElse(500)

  private val abiNames = Array(
    "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1",
    "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7",
    "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
    "t3", "t4", "t5", "t6"
  )

  private def reg(i: Int): String = abiNames(i & 31)

  private def signExtend(value: Int, bits: Int): Int = (value << (32 - bits)) >> (32 - bits)

  private def toUnsigned32(x: scala.math.BigInt): Long = java.lang.Integer.toUnsignedLong(x.intValue)

  private def fmtMnemonic(mn: String, ops: String): String = {
    val left = String.format("%-7s", mn)
    if (ops == null || ops.isEmpty) left.trim else s"$left $ops"
  }

  private def csrName(csr: Int): String = csr match {
    case 0x300 => "mstatus"
    case 0x301 => "misa"
    case 0x304 => "mie"
    case 0x305 => "mtvec"
    case 0x340 => "mscratch"
    case 0x341 => "mepc"
    case 0x342 => "mcause"
    case 0x343 => "mtval"
    case 0x344 => "mip"
    case 0xF14 => "mhartid"
    case _     => f"0x$csr%03x"
  }

  private def disasm(pc: Long, inst: Long): String = {
    val opcode = (inst & 0x7fL).toInt
    val rd = ((inst >>> 7) & 0x1fL).toInt
    val funct3 = ((inst >>> 12) & 0x7L).toInt
    val rs1 = ((inst >>> 15) & 0x1fL).toInt
    val rs2 = ((inst >>> 20) & 0x1fL).toInt
    val funct7 = ((inst >>> 25) & 0x7fL).toInt

    val immI = signExtend((inst >>> 20).toInt, 12)
    val immS = signExtend((((inst >>> 25) & 0x7fL).toInt << 5) | (((inst >>> 7) & 0x1fL).toInt), 12)
    val immB = signExtend(
      ((((inst >>> 31) & 0x1L).toInt) << 12) |
        ((((inst >>> 7) & 0x1L).toInt) << 11) |
        ((((inst >>> 25) & 0x3fL).toInt) << 5) |
        ((((inst >>> 8) & 0xfL).toInt) << 1),
      13
    )
    val immU = (inst & 0xfffff000L)
    val immJ = signExtend(
      ((((inst >>> 31) & 0x1L).toInt) << 20) |
        ((((inst >>> 12) & 0xffL).toInt) << 12) |
        ((((inst >>> 20) & 0x1L).toInt) << 11) |
        ((((inst >>> 21) & 0x3ffL).toInt) << 1),
      21
    )

    opcode match {
      case 0x37 =>
        fmtMnemonic("lui", s"${reg(rd)}, 0x${(immU >>> 12).toHexString}")
      case 0x17 =>
        fmtMnemonic("auipc", s"${reg(rd)}, 0x${(immU >>> 12).toHexString}")

      case 0x6f =>
        if (rd == 0) fmtMnemonic("j", s"pc + 0x${immJ.toHexString}")
        else fmtMnemonic("jal", s"${reg(rd)}, pc + 0x${immJ.toHexString}")

      case 0x67 =>
        if (rd == 0 && rs1 == 1 && immI == 0) fmtMnemonic("ret", "")
        else if (rd == 0 && immI == 0) fmtMnemonic("jr", s"${reg(rs1)}")
        else fmtMnemonic("jalr", s"${reg(rd)}, ${reg(rs1)}, $immI")

      case 0x63 =>
        val mn = funct3 match {
          case 0 => "beq"
          case 1 => "bne"
          case 4 => "blt"
          case 5 => "bge"
          case 6 => "bltu"
          case 7 => "bgeu"
          case _ => "b?"
        }
        fmtMnemonic(mn, s"${reg(rs1)}, ${reg(rs2)}, pc + $immB")

      case 0x03 =>
        val mn = funct3 match {
          case 0 => "lb"
          case 1 => "lh"
          case 2 => "lw"
          case 4 => "lbu"
          case 5 => "lhu"
          case _ => "l?"
        }
        fmtMnemonic(mn, s"${reg(rd)}, $immI(${reg(rs1)})")

      case 0x23 =>
        val mn = funct3 match {
          case 0 => "sb"
          case 1 => "sh"
          case 2 => "sw"
          case _ => "s?"
        }
        fmtMnemonic(mn, s"${reg(rs2)}, $immS(${reg(rs1)})")

      case 0x13 =>
        funct3 match {
          case 0 =>
            if (rd == 0 && rs1 == 0 && immI == 0) fmtMnemonic("nop", "")
            else if (rs1 == 0) fmtMnemonic("li", s"${reg(rd)}, $immI")
            else if (immI == 0) fmtMnemonic("mv", s"${reg(rd)}, ${reg(rs1)}")
            else fmtMnemonic("addi", s"${reg(rd)}, ${reg(rs1)}, $immI")
          case 2 => fmtMnemonic("slti", s"${reg(rd)}, ${reg(rs1)}, $immI")
          case 3 => fmtMnemonic("sltiu", s"${reg(rd)}, ${reg(rs1)}, $immI")
          case 4 => fmtMnemonic("xori", s"${reg(rd)}, ${reg(rs1)}, $immI")
          case 6 => fmtMnemonic("ori", s"${reg(rd)}, ${reg(rs1)}, $immI")
          case 7 => fmtMnemonic("andi", s"${reg(rd)}, ${reg(rs1)}, $immI")
          case 1 =>
            val shamt = ((inst >>> 20) & 0x1fL).toInt
            fmtMnemonic("slli", s"${reg(rd)}, ${reg(rs1)}, $shamt")
          case 5 =>
            val shamt = ((inst >>> 20) & 0x1fL).toInt
            if (funct7 == 0x20) fmtMnemonic("srai", s"${reg(rd)}, ${reg(rs1)}, $shamt")
            else fmtMnemonic("srli", s"${reg(rd)}, ${reg(rs1)}, $shamt")
          case _ => fmtMnemonic("opimm?", "")
        }

      case 0x33 =>
        if (funct7 == 0x01) {
          val mn = funct3 match {
            case 0 => "mul"
            case 1 => "mulh"
            case 2 => "mulhsu"
            case 3 => "mulhu"
            case 4 => "div"
            case 5 => "divu"
            case 6 => "rem"
            case 7 => "remu"
            case _ => "m?"
          }
          fmtMnemonic(mn, s"${reg(rd)}, ${reg(rs1)}, ${reg(rs2)}")
        } else {
          val mn = funct3 match {
            case 0 => if (funct7 == 0x20) "sub" else "add"
            case 1 => "sll"
            case 2 => "slt"
            case 3 => "sltu"
            case 4 => "xor"
            case 5 => if (funct7 == 0x20) "sra" else "srl"
            case 6 => "or"
            case 7 => "and"
            case _ => "op?"
          }
          fmtMnemonic(mn, s"${reg(rd)}, ${reg(rs1)}, ${reg(rs2)}")
        }

      case 0x0f =>
        fmtMnemonic("fence", "")

      case 0x73 =>
        if (funct3 == 0 && immI == 0) fmtMnemonic("ecall", "")
        else if (funct3 == 0 && immI == 1) fmtMnemonic("ebreak", "")
        else {
          val csr = ((inst >>> 20) & 0xfffL).toInt
          val csrN = csrName(csr)
          funct3 match {
            case 1 => fmtMnemonic("csrrw", s"${reg(rd)}, $csrN, ${reg(rs1)}")
            case 2 => if (rs1 == 0) fmtMnemonic("csrr", s"${reg(rd)}, $csrN") else fmtMnemonic("csrrs", s"${reg(rd)}, $csrN, ${reg(rs1)}")
            case 3 => if (rs1 == 0) fmtMnemonic("csrr", s"${reg(rd)}, $csrN") else fmtMnemonic("csrrc", s"${reg(rd)}, $csrN, ${reg(rs1)}")
            case 5 => fmtMnemonic("csrrwi", s"${reg(rd)}, $csrN, ${rs1}")
            case 6 => fmtMnemonic("csrrsi", s"${reg(rd)}, $csrN, ${rs1}")
            case 7 => fmtMnemonic("csrrci", s"${reg(rd)}, $csrN, ${rs1}")
            case _ => fmtMnemonic("system?", "")
          }
        }

      case _ =>
        fmtMnemonic("unknown", f"0x$inst%08x")
    }
  }

  private def loadFirmwareHex(path: String): Map[Long, Int] = {
    val p = Paths.get(path)
    if (!Files.exists(p)) {
      val cwd = Paths.get("").toAbsolutePath
      throw new FileNotFoundException(s"Firmware not found: $path (cwd=$cwd)")
    }

    val lines = Files.readAllLines(p).asScala.toVector
    val mem = mutable.LinkedHashMap.empty[Long, Int]

    var haveAt = false
    var wordAddr: Long = 0x80000000L

    def parseWordToken(tok: String): Option[Int] = {
      val t = tok.trim
      if (t.isEmpty) None
      else if (t.matches("[01]{32}")) Some(java.lang.Long.parseUnsignedLong(t, 2).toInt)
      else if (t.matches("[0-9a-fA-F]{1,8}")) Some(java.lang.Long.parseUnsignedLong(t, 16).toInt)
      else None
    }

    for (raw <- lines) {
      val s0 = raw.trim
      if (s0.nonEmpty && !s0.startsWith("#") && !s0.startsWith("//")) {
        if (s0.startsWith("@")) {
          haveAt = true
          val a = java.lang.Long.parseUnsignedLong(s0.drop(1).trim, 16)
          wordAddr = if (a >= 0x80000000L) (a >>> 2) else a
        } else if (s0.startsWith(":")) {
          val rec = s0.drop(1)
          if (rec.length >= 10) {
            val len = Integer.parseInt(rec.substring(0, 2), 16)
            val addr = Integer.parseInt(rec.substring(2, 6), 16)
            val typ = Integer.parseInt(rec.substring(6, 8), 16)
            if (typ == 0 && rec.length >= 8 + len * 2) {
              val base = if (haveAt) (wordAddr << 2) else 0L
              val byteAddr0 = base + addr.toLong
              val bytes = (0 until len).map { i =>
                Integer.parseInt(rec.substring(8 + i * 2, 10 + i * 2), 16)
              }
              var i = 0
              while (i + 3 < bytes.length) {
                val b0 = bytes(i)
                val b1 = bytes(i + 1)
                val b2 = bytes(i + 2)
                val b3 = bytes(i + 3)
                val w = (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24)
                val wa = ((byteAddr0 + i.toLong) >>> 2)
                mem.update(wa, w)
                i += 4
              }
            } else if (typ == 4 && rec.length >= 12) {
              val hi = Integer.parseInt(rec.substring(8, 12), 16)
              wordAddr = (hi.toLong << 16) >>> 2
              haveAt = true
            }
          }
        } else {
          val tokens = s0.split("\\s+")
          var any = false
          tokens.foreach { tok =>
            parseWordToken(tok).foreach { w =>
              any = true
              mem.update(wordAddr, w)
              wordAddr += 1
            }
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
    }

    if (mem.isEmpty) {
      throw new RuntimeException(s"Parsed 0 words from firmware: $path")
    }

    mem.toMap
  }

  private def preloadMem(dut: CpuTop, words: Map[Long, Int]): Unit = {
    val iDepth = dut.iMem.mem.wordCount
    val dDepth = dut.dMem.mem.wordCount
    if (iDepth <= 0 || dDepth <= 0) throw new RuntimeException("Memory depth is not positive")

    words.foreach { case (wa, w) =>
      val idxI = (java.lang.Long.remainderUnsigned(wa, iDepth.toLong)).toInt
      val idxD = (java.lang.Long.remainderUnsigned(wa, dDepth.toLong)).toInt
      val bi = scala.math.BigInt(java.lang.Integer.toUnsignedLong(w))
      dut.iMem.mem.setBigInt(idxI, bi)
      dut.dMem.mem.setBigInt(idxD, bi)
    }
  }

  private val firmwareWords = loadFirmwareHex(firmwarePath)

  SimConfig
    .withWave
    .compile {
      val dut = new CpuTop(config)
      dut.pc.io.bus.pc.simPublic()
      dut.iMem.mem.simPublic()
      dut.iMem.io.bus.readData.simPublic()
      dut.dMem.mem.simPublic()
      dut.dMem.io.bus.readData.simPublic()
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

      var i = 0
      var samePcStreak = 0
      var lastPc: Long = -1L

      while (i < maxInstructions && samePcStreak < 2000) {
        val pc = toUnsigned32(dut.pc.io.bus.pc.toBigInt)
        val inst = toUnsigned32(dut.iMem.io.bus.readData.toBigInt)
        val asm = disasm(pc, inst)
        val line = f"core   0: 0x$pc%08x (0x$inst%08x) $asm"
        println(line)
        out.println(line)

        if (pc == lastPc) samePcStreak += 1 else samePcStreak = 0
        lastPc = pc

        dut.clockDomain.waitRisingEdge()
        i += 1
      }

      out.flush()
      out.close()
    }
}
