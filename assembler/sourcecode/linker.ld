ENTRY(_start)

/* 1. 这里的 ORIGIN 改为 0x00000000，LENGTH 也要确保够大 */
MEMORY {
  RAM (rwx) : ORIGIN = 0x00000000, LENGTH = 128M
}

SECTIONS {
    /* 2. 强制将当前地址指针设置为 0 */
    . = 0x00000000;

    .text : {
        *(.text.entry) 
        *(.text .text.*)
    } > RAM

    .rodata : {
        *(.rodata .rodata.*)
    } > RAM

    .data : {
        *(.data .data.*)
    } > RAM

    .bss : {
        *(.bss .bss.*)
    } > RAM
}