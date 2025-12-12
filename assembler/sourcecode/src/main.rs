#![no_std]
#![no_main]

use core::hint::black_box;
use core::panic::PanicInfo;

#[panic_handler]
fn panic(_info: &PanicInfo) -> ! {
    loop {}
}

#[unsafe(no_mangle)]
pub extern "C" fn _start() -> ! {
    let mut a = black_box(1);
    let b = black_box(2);
    let c = a + b;
    black_box(c);

    loop {
        a += 1;
    }
}
