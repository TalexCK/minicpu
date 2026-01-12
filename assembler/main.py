import os
import subprocess
import re
from .utils import write_file
from .assembler import encode_file

script_dir = os.path.dirname(os.path.abspath(__file__))


def build_and_dump() -> None:
    cargo_cmd = ["cargo", "build", "--release", "--target", "riscv32i-unknown-none-elf"]

    output_asm_path = os.path.join(script_dir, "firmware.asm")

    elf_path = os.path.join(
        script_dir,
        "sourcecode",
        "target",
        "riscv32i-unknown-none-elf",
        "release",
        "sourcecode",
    )

    dump_cmd = [
        "rust-objdump",
        "-d",
        elf_path,
        "-M",
        "no-aliases",
        "--no-show-raw-insn",
    ]

    print(f"--- Starting Cargo Build ---")
    subprocess.run(cargo_cmd, cwd=os.path.join(script_dir, "sourcecode"), check=True)

    print("--- Processing Disassembly to ASM ---")

    output = subprocess.check_output(dump_cmd, text=True)

    processed_lines = []

    line_pattern = re.compile(r"^\s*([0-9a-f]+):\s+(.*)$")
    target_pattern = re.compile(r"(0x[0-9a-f]+)")

    branch_mnemonics = {
        "beq",
        "bne",
        "blt",
        "bge",
        "bltu",
        "bgeu",
        "jal",
        "jalr",
    }

    for line in output.splitlines():
        if any(ignore in line for ignore in ["file format", "Disassembly"]):
            continue
        if line.strip().endswith(">:"):
            continue

        match = line_pattern.match(line)
        if match:
            pc_str = match.group(1)
            raw_instruction = match.group(2)

            instruction = re.sub(r"\s*<.*>", "", raw_instruction).strip()

            if not instruction:
                continue

            parts = instruction.split()
            mnemonic = parts[0] if parts else ""

            if mnemonic in branch_mnemonics:
                current_pc = int(pc_str, 16)
                t_match = target_pattern.search(instruction)

                if t_match:
                    target_hex = t_match.group(1)
                    target_addr = int(target_hex, 16)
                    diff = target_addr - current_pc

                    if -1048576 < diff < 1048576:
                        sign = "+" if diff >= 0 else ""
                        rel_str = f"{sign}{diff}"
                        instruction = instruction.replace(target_hex, rel_str)

            processed_lines.append(instruction)

    write_file(output_asm_path, [line + "\n" for line in processed_lines])

    print(f"--- ASM generated at {output_asm_path} ---")


def generate_hex() -> None:
    build_and_dump()
    encode_file(os.path.join(script_dir, "firmware.asm"))


def generate_hex_directly() -> None:
    encode_file(os.path.join(script_dir, "firmware.asm"))


if __name__ == "__main__":
    build_and_dump()

    print("--- Starting Assembler ---")
    encode_file(os.path.join(script_dir, "firmware.asm"))
