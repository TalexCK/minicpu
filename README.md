# MiniCPU

A simplified CPU implementation featuring a custom datapath and controller, written in SpinalHDL.

## Prerequisites

Ensure you have the following tools installed:

- **Python 3**
- **Rust Toolchain**: For sourcecode.
- **Scala & SBT**: For SpinalHDL.
- **Verilator**: For simulation backend.
- **Spike (riscv-isa-sim)**: For instruction set verification (optional, used by `checker.py`).

## Project Structure

- `assembler/`: Contains the Rust source code for the firmware and Python scripts to compile/assemble it into machine code.
- `spinal/`: The SpinalHDL hardware description project.
- `checker.py`: A script to verify the CPU execution against the Spike simulator.

## Usage

### 1. Write & Compile Firmware

The firmware is written in Rust.

1.  Navigate to `assembler/sourcecode/src/`.
2.  Copy `main.rs.example` to `main.rs` (if not already done).
3.  Modify `main.rs` with your desired code.
4.  Run the assembler from the root directory:

    ```bash
    python3 -m assembler.main
    ```

    This will generate `assembler/firmware.asm` (assembly) and `assembler/firmware.hex` (machine code).

### 2. Run Simulation

Simulate the CPU running your firmware using Verilator.

1.  Navigate to the `spinal` directory:
    ```bash
    cd spinal
    ```
2.  Run the simulation:

    ```bash
    sbt "runMain minicpu.CpuTopSim"
    ```

    This will execute the firmware and generate a commit log at `logs/minicpu.log`.

### 3. Generate Verilog

To generate the Verilog RTL for synthesis or implementation:

```bash
cd spinal
sbt "runMain minicpu.CpuTopVerilog"
```

The output file `CpuTop.v` will be generated in `spinal/hw/gen/`.

### 4. Verification (Optional)

You can verify the CPU's execution against the Spike simulator.

```bash
python3 checker.py
```

This script converts the hex file to binary, runs it in Spike, and compares the execution logs.

**args:**

- `-d`: Generate hex from esisting asm file instead of compiled Rust code.
