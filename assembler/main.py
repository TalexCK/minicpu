import os
import subprocess
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

    dump_cmd = (
        f"rust-objdump -d {elf_path} -M numeric,no-aliases --no-show-raw-insn | "
        "grep -vE 'file format|Disassembly| <.*>:' | "
        "sed -E 's/^[[:space:]]*[0-9a-f]+:[[:space:]]*//' | "
        "sed -E 's/<.*>//' | "
        f"sed '/^$/d' > {output_asm_path}"
    )

    print(f"--- Starting Cargo Build ---")
    subprocess.run(cargo_cmd, cwd=os.path.join(script_dir, "sourcecode"), check=True)

    print("--- Processing Disassembly to ASM ---")
    subprocess.run(dump_cmd, shell=True, check=True)

    print(f"--- ASM generated at {output_asm_path} ---")


if __name__ == "__main__":
    build_and_dump()

    print("--- Starting Assembler ---")
    encode_file(os.path.join(script_dir, "firmware.asm"))
