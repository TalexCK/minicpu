ENTRY(_start)
SECTIONS {
  . = 0x80000000;
  .text : { _start = .; *(.text*) *(.rodata*) *(.data*) }
  .bss : { *(.bss*) *(COMMON) }
}
