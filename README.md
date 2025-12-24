# MiniCPU

A simplified CPU, covering datapath and controller.

## QuickStart

### assembler

1. Copy `assembler/sourcecode/src/main.rs.example` to `assembler/sourcecode/src/main.rs`.

2. Modify the code in `assembler/sourcecode/src/main.rs`.

3. Run `python3 -m assembler.main` in the root directory to generate assembly code and machine code.

### spinal

#### generate verilog diagram

Run `sbt "runMain minicpu.CpuTopVerilog"` in `/path/to/project/spinal` to generate verilog diagram
