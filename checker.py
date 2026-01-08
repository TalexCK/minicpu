import os
import re
import struct
import subprocess
import sys
from pathlib import Path
from typing import List, Optional, Tuple

import assembler.main as assembler

ABI_NAMES = [
    "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1",
    "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3",
    "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
]

RESET_VECTOR = 0x80000000


def run_checked(cmd: List[str], cwd: Optional[Path] = None, env: Optional[dict] = None) -> None:
    p = subprocess.run(cmd, cwd=str(cwd) if cwd else None, env=env)
    if p.returncode != 0:
        raise SystemExit(p.returncode)


def text_bits_to_binary(input_path: Path, output_path: Path) -> None:
    content = input_path.read_text(encoding="utf-8", errors="ignore")
    bits = "".join(c for c in content if c in "01")
    with output_path.open("wb") as f_out:
        for i in range(0, len(bits), 32):
            chunk = bits[i : i + 32]
            if len(chunk) == 32:
                val = int(chunk, 2) & 0xFFFFFFFF
                f_out.write(struct.pack("<I", val))


def build_elf_from_hex(base_dir: Path) -> Path:
    input_hex = base_dir / "assembler" / "firmware.hex"
    bin_file = base_dir / "firmware.bin"
    rel_elf = base_dir / "firmware_rel.elf"
    final_elf = base_dir / "firmware.elf"
    link_script = base_dir / "linker.ld"

    if not input_hex.exists():
        print(f"[!] File not found: {input_hex}")
        raise SystemExit(1)

    text_bits_to_binary(input_hex, bin_file)

    objcopy = os.getenv("OBJCOPY", "rust-objcopy")
    ld = os.getenv("LD", "rust-lld")

    run_checked(
        [
            objcopy,
            "-I",
            "binary",
            "-O",
            "elf32-littleriscv",
            "--rename-section",
            ".data=.text,contents,alloc,load,readonly,code",
            str(bin_file),
            str(rel_elf),
        ]
    )

    run_checked(
        [
            ld,
            "-flavor",
            "gnu",
            "-m",
            "elf32lriscv",
            "-T",
            str(link_script),
            "-o",
            str(final_elf),
            str(rel_elf),
        ]
    )

    return final_elf


def run_spinal_sim(base_dir: Path, max_cycles: int) -> None:
    script_dir = Path(__file__).resolve().parent
    spinal_path = script_dir / "spinal"

    sim_env = os.environ.copy()
    current_path = sim_env.get("PATH", "")
    sim_env["PATH"] = f"/usr/bin:/bin:/usr/sbin:/sbin:{current_path}"

    (base_dir / "logs").mkdir(parents=True, exist_ok=True)

    cmd = [
        "sbt",
        "-Dfirmware=../assembler/firmware.hex",
        "-DcommitLog=../logs/minicpu.log",
        f"-DmaxInstructions={max_cycles}",
        "runMain minicpu.CpuTopSim",
    ]
    run_checked(cmd, cwd=spinal_path, env=sim_env)


def run_spike_capture(final_elf: Path, timeout_s: float) -> List[str]:
    spike = os.getenv("SPIKE", "spike")
    isa = os.getenv("SPIKE_ISA", "rv32imac")
    cmd = [spike, "-l", "--log-commits", f"--isa={isa}", str(final_elf)]
    try:
        p = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=timeout_s,
            check=False,
        )
        out = p.stdout.decode("utf-8", errors="ignore")
        return out.splitlines()
    except subprocess.TimeoutExpired as e:
        out = (e.stdout or b"").decode("utf-8", errors="ignore")
        return out.splitlines()


def parse_spike_events(lines: List[str]) -> List[Tuple[int, int, List[Tuple[int, int]]]]:
    instr_re = re.compile(r"^core\s+\d+:\s+(0x[0-9a-fA-F]+)\s+\((0x[0-9a-fA-F]+)\)\s+")
    commit_gpr_re = re.compile(
        r"^core\s+\d+:\s+\d+\s+(0x[0-9a-fA-F]+)\s+\((0x[0-9a-fA-F]+)\)\s+x\s*(\d+)\s+(0x[0-9a-fA-F]+)"
    )

    events: List[Tuple[int, int, List[Tuple[int, int]]]] = []
    cur_pc: Optional[int] = None
    cur_inst: Optional[int] = None
    cur_writes: List[Tuple[int, int]] = []

    for line in lines:
        s = line.strip()
        if not s:
            continue

        mi = instr_re.match(s)
        if mi:
            if cur_pc is not None and cur_inst is not None:
                events.append((cur_pc, cur_inst, cur_writes))
            cur_pc = int(mi.group(1), 16) & 0xFFFFFFFF
            cur_inst = int(mi.group(2), 16) & 0xFFFFFFFF
            cur_writes = []
            continue

        mc = commit_gpr_re.match(s)
        if mc:
            pc = int(mc.group(1), 16) & 0xFFFFFFFF
            inst = int(mc.group(2), 16) & 0xFFFFFFFF
            rd = int(mc.group(3))
            val = int(mc.group(4), 16) & 0xFFFFFFFF

            if cur_pc == pc and cur_inst == inst:
                cur_writes.append((rd, val))
            else:
                found = False
                for j in range(len(events) - 1, max(-1, len(events) - 32), -1):
                    epc, einst, ew = events[j]
                    if epc == pc and einst == inst:
                        ew.append((rd, val))
                        found = True
                        break
                if not found:
                    if cur_pc is not None and cur_inst is not None:
                        events.append((cur_pc, cur_inst, cur_writes))
                    cur_pc, cur_inst, cur_writes = pc, inst, [(rd, val)]
            continue

    if cur_pc is not None and cur_inst is not None:
        events.append((cur_pc, cur_inst, cur_writes))

    return events


def format_dump_line(time_no: int, pc: int, regs: List[int]) -> str:
    parts = [f"{ABI_NAMES[i]}=0x{regs[i] & 0xFFFFFFFF:08x}" for i in range(32)]
    return f"--time:no.{time_no} pc=0x{pc & 0xFFFFFFFF:08x} " + " ".join(parts)


def write_spike_log(base_dir: Path, events: List[Tuple[int, int, List[Tuple[int, int]]]], max_cycles: int) -> None:
    start_idx = 0
    for i, (pc, _, _) in enumerate(events):
        if pc == RESET_VECTOR:
            start_idx = i
            break
    events = events[start_idx:]

    regs = [0] * 32
    out_lines: List[str] = []

    n = min(max_cycles, len(events))
    for i in range(n):
        pc, inst, writes = events[i]
        for rd, val in writes:
            if rd != 0:
                regs[rd] = val & 0xFFFFFFFF
        regs[0] = 0
        if i + 1 < len(events):
            next_pc = events[i + 1][0] & 0xFFFFFFFF
        else:
            next_pc = (pc + 4) & 0xFFFFFFFF
        out_lines.append(format_dump_line(i + 1, next_pc, regs))

    log_dir = base_dir / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    (log_dir / "spike.log").write_text("\n".join(out_lines) + ("\n" if out_lines else ""), encoding="utf-8")


def parse_dump_line(line: str) -> Optional[Tuple[int, int, List[int]]]:
    m = re.match(r"^--time:no\.(\d+)\s+pc=(0x[0-9a-fA-F]+)\s+(.*)$", line.strip())
    if not m:
        return None
    t = int(m.group(1))
    pc = int(m.group(2), 16) & 0xFFFFFFFF
    rest = m.group(3)
    kv = dict(re.findall(r"([a-z0-9]+)=0x([0-9a-fA-F]{8})", rest))
    regs = []
    for name in ABI_NAMES:
        if name not in kv:
            return None
        regs.append(int(kv[name], 16) & 0xFFFFFFFF)
    regs[0] = 0
    return t, pc, regs


def compare_logs(base_dir: Path) -> None:
    a_path = base_dir / "logs" / "minicpu.log"
    b_path = base_dir / "logs" / "spike.log"

    if not a_path.exists() or not b_path.exists():
        print("[!] Missing log file(s).")
        print(f"    minicpu: {a_path} exists={a_path.exists()}")
        print(f"    spike  : {b_path} exists={b_path.exists()}")
        return

    a_lines = [x.strip() for x in a_path.read_text(encoding="utf-8", errors="ignore").splitlines() if x.strip()]
    b_lines = [x.strip() for x in b_path.read_text(encoding="utf-8", errors="ignore").splitlines() if x.strip()]

    a_parsed = [parse_dump_line(x) for x in a_lines]
    b_parsed = [parse_dump_line(x) for x in b_lines]

    if not a_parsed or not b_parsed:
        print("[!] Log format parse failed.")
        return
    if any(x is None for x in a_parsed[: min(5, len(a_parsed))]) or any(
            x is None for x in b_parsed[: min(5, len(b_parsed))]):
        print("[!] Log format parse failed.")
        return

    a_parsed = [x for x in a_parsed if x is not None]
    b_parsed = [x for x in b_parsed if x is not None]

    n = min(len(a_parsed), len(b_parsed))
    if n == 0:
        print("[!] One of the logs is empty after parsing.")
        return

    base_a = a_parsed[0][2]
    base_b = b_parsed[0][2]

    active_a = [False] * 32
    active_b = [False] * 32
    active_a[0] = True
    active_b[0] = True

    err = 0
    compared_regs_ever = [False] * 32

    for i in range(n):
        ta, pca, ra = a_parsed[i]
        tb, pcb, rb = b_parsed[i]

        for r in range(1, 32):
            if (not active_a[r]) and (ra[r] != base_a[r]):
                active_a[r] = True
            if (not active_b[r]) and (rb[r] != base_b[r]):
                active_b[r] = True

        pc_mismatch = (pca != pcb)
        first_diff = None

        for r in range(1, 32):
            if active_a[r] or active_b[r]:
                compared_regs_ever[r] = True
                if ra[r] != rb[r]:
                    first_diff = r
                    break

        if pc_mismatch or (first_diff is not None):
            print(f"[!] Mismatch at line {i + 1}:")
            print(f"    Spinal: {a_lines[i]}")
            print(f"    Spike : {b_lines[i]}")
            if pc_mismatch:
                print(f"    PC diff: spinal=0x{pca:08x} spike=0x{pcb:08x}")
            if first_diff is not None:
                r = first_diff
                print(f"    First reg diff: {ABI_NAMES[r]} spinal=0x{ra[r]:08x} spike=0x{rb[r]:08x}")
            err += 1

    if len(a_parsed) != len(b_parsed):
        print(f"[!] Warning: Log lengths differ. Spinal: {len(a_parsed)}, Spike: {len(b_parsed)}")

    used = [ABI_NAMES[r] for r in range(1, 32) if compared_regs_ever[r]]
    if used:
        print("[-] Compared regs:", ", ".join(used))

    if err == 0:
        print("[-] No Mismatches Found, Run Correctly!")
    else:
        print(f"[-] {err} Mismatches Found!")


def cleanup(base_dir: Path) -> None:
    for name in ["firmware_rel.elf", "firmware.bin", "firmware.elf"]:
        p = base_dir / name
        if p.exists():
            try:
                p.unlink()
            except OSError:
                pass


def main() -> None:
    base_dir = Path.cwd()

    if "-d" in sys.argv:
        assembler.generate_hex_directly()
    else:
        assembler.generate_hex()

    max_cycles = int(os.getenv("MAX_CYCLES", "500"))
    spike_timeout = float(os.getenv("SPIKE_TIMEOUT", "0.5"))

    run_spinal_sim(base_dir, max_cycles)

    final_elf = build_elf_from_hex(base_dir)
    spike_lines = run_spike_capture(final_elf, spike_timeout)
    events = parse_spike_events(spike_lines)
    write_spike_log(base_dir, events, max_cycles)

    compare_logs(base_dir)
    cleanup(base_dir)


if __name__ == "__main__":
    main()
