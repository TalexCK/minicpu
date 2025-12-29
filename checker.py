import os
import re
import struct
import subprocess
import sys
from pathlib import Path

import assembler.main as assembler


def text_bits_to_binary(input_path, output_path):
    try:
        print(f"[-] Processing: {input_path}")
        with open(input_path, "r", encoding="utf-8") as f:
            content = f.read()

        bits = "".join(c for c in content if c in "01")
        pack_format = "<I"

        with open(output_path, "wb") as f_out:
            for i in range(0, len(bits), 32):
                chunk = bits[i: i + 32]
                if len(chunk) == 32:
                    val = int(chunk, 2)
                    f_out.write(struct.pack(pack_format, val))
    except Exception as e:
        print(f"[!] Error: {e}")
        sys.exit(1)


def run_command(cmd):
    try:
        subprocess.run(cmd, check=True)
    except subprocess.CalledProcessError as e:
        sys.exit(e.returncode)


def main():
    base_dir = Path.cwd()
    input_hex = base_dir / "assembler" / "firmware.hex"
    bin_file = base_dir / "firmware.bin"
    rel_elf = base_dir / "firmware_rel.elf"
    final_elf = base_dir / "firmware.elf"
    link_script = base_dir / "linker.ld"

    log_dir = base_dir / "logs"
    log_file = log_dir / "spike.log"

    OBJCOPY = "rust-objcopy"
    LD = "rust-lld"
    SPIKE = "spike"

    if not input_hex.exists():
        print(f"[!] File not found: {input_hex}")
        sys.exit(1)

    text_bits_to_binary(input_hex, bin_file)

    cmd_objcopy = [
        OBJCOPY,
        "-I",
        "binary",
        "-O",
        "elf32-littleriscv",
        "--rename-section",
        ".data=.text,contents,alloc,load,readonly,code",
        str(bin_file),
        str(rel_elf),
    ]
    run_command(cmd_objcopy)

    cmd_ld = [
        LD,
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
    run_command(cmd_ld)

    log_dir.mkdir(parents=True, exist_ok=True)
    cmd_spike = [SPIKE, "-d", "--isa=rv32imac", str(final_elf)]

    print(f"[-] Running Spike -> {log_file}")

    with open(log_file, "w") as f_log:
        try:
            subprocess.run(
                cmd_spike,
                stdout=f_log,
                stderr=subprocess.STDOUT,
                timeout=0.1,
                check=True,
            )
        except subprocess.TimeoutExpired:
            print("[*] Timeout reached. Terminating.")
        except subprocess.CalledProcessError:
            print("[!] Spike error.")
    process_spike_log(log_file, log_file)
    run_spinal_sim()

    files_to_remove = [rel_elf, bin_file, final_elf]
    for f_path in files_to_remove:
        if f_path.exists():
            try:
                f_path.unlink()
            except OSError:
                pass


def process_spike_log(input_file, output_file):
    try:
        with open(input_file, "r") as f:
            lines = f.readlines()

        start_line = 0
        for line in lines:
            if line.startswith("(spike) core   0: 0x80000000"):
                start_line = lines.index(line)
                break

        target_lines = lines[start_line: start_line + 500]

        cleaned_lines = []
        for line in target_lines:
            new_line = line.replace("(spike) ", "", 1)
            cleaned_lines.append(new_line)

        with open(output_file, "w") as f:
            f.writelines(cleaned_lines)

        print(f"[-] Log processed: Extracted lines to {output_file}")

    except FileNotFoundError:
        print(f"[!] Error: File {input_file} not found.")
    except Exception as e:
        print(f"[!] Error processing log: {e}")


def run_spinal_sim():
    print("--- Starting SpinalHDL Simulation ---")

    script_dir = os.path.dirname(os.path.abspath(__file__))

    spinal_path = os.path.join(script_dir, "spinal")

    sim_env = os.environ.copy()
    current_path = sim_env.get("PATH", "")
    sim_env["PATH"] = f"/usr/bin:/bin:/usr/sbin:/sbin:{current_path}"

    cmd = [
        "sbt",
        "runMain minicpu.CpuTopSim",
        "-Dfirmware=../assembler/firmware.hex",
        "-DcommitLog=commit.log",
        "-DmaxInstructions=500",
    ]

    try:
        subprocess.run(cmd, cwd=spinal_path, env=sim_env, check=True)
        print("[-] SpinalHDL Simulation Finished Successfully")

    except subprocess.CalledProcessError:
        print("[!] SpinalHDL Simulation Failed")
        exit(1)
    except FileNotFoundError:
        print("[!] Error: 'sbt' command not found.")
        exit(1)


def read_file_as_list(path: str, encoding: str = "utf-8") -> list[str]:
    if not os.path.exists(path):
        print(f"[!] Log file not found: {path}")
        return []
    with open(path, "r", encoding=encoding) as f:
        return [i.strip() for i in f.read().splitlines() if i.strip()]


def parse_line_values(line: str):
    line = line.strip()
    pattern = re.compile(
        r"core\s+\d+:\s+(0x[0-9a-fA-F]+)\s+\((0x[0-9a-fA-F]+)\)\s+(.*)"
    )
    match = pattern.search(line)

    if not match:
        return None

    pc_val = int(match.group(1), 16)
    inst_val = int(match.group(2), 16)
    rest_str = match.group(3).strip()

    parts = rest_str.split(maxsplit=1)
    mnemonic = parts[0]
    operands = parts[1] if len(parts) > 1 else ""

    return pc_val, inst_val, mnemonic, operands


def normalize_operand_value(op_str: str) -> str:
    token_pattern = re.compile(r"([+-])?\s*(0x[0-9a-fA-F]+|\d+)")

    def replace_num(match):
        sign_str = match.group(1)
        num_str = match.group(2)

        try:
            if num_str.lower().startswith("0x"):
                val = int(num_str, 16)
            else:
                val = int(num_str, 10)

            if sign_str == "-":
                val = -val

            val_u32 = val & 0xFFFFFFFF

            return f"{{NUM:{val_u32}}}"
        except ValueError:
            return match.group(0)

    normalized = token_pattern.sub(replace_num, op_str)
    normalized = normalized.replace(" ", "")
    return normalized


def is_log_line_equal(line1: str, line2: str) -> bool:
    parsed1 = parse_line_values(line1)
    parsed2 = parse_line_values(line2)

    if not parsed1 or not parsed2:
        return line1.strip() == line2.strip()

    pc1, inst1, mn1, ops1 = parsed1
    pc2, inst2, mn2, ops2 = parsed2

    if pc1 != pc2 or inst1 != inst2:
        return False

    if mn1.lower() != mn2.lower():
        return False

    norm_ops1 = normalize_operand_value(ops1)
    norm_ops2 = normalize_operand_value(ops2)

    return norm_ops1 == norm_ops2


def check_logs():
    print("--- Starting Log Check ---")
    minicpu_log = read_file_as_list("logs/minicpu.log")
    spike_log = read_file_as_list("logs/spike.log")

    compare_len = min(len(minicpu_log), len(spike_log))
    if compare_len == 0:
        print("[!] One of the logs is empty!")
        return

    err_num = 0
    for i in range(compare_len):
        line_spinal = minicpu_log[i]
        line_spike = spike_log[i]

        if not is_log_line_equal(line_spinal, line_spike):
            print(f"[!] Mismatch at line {i + 1}:")
            print(f"    Spinal: {line_spinal}")
            print(f"    Spike : {line_spike}")
            err_num += 1

    if len(minicpu_log) != len(spike_log):
        print(
            f"[!] Warning: Log lengths differ. Spinal: {len(minicpu_log)}, Spike: {len(spike_log)}"
        )

    if err_num == 0:
        print("[-] No Mismatches Found, Run Correctly!")
    else:
        print(f"[-] {err_num} Mismatches Found!")


if __name__ == "__main__":
    assembler.generate_hex()
    main()
    check_logs()
