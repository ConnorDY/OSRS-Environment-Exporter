uint bit_reverse8(uint val) {
    val = val << 4 & 0xF0u | val >> 4 & 0x0Fu;
    val = val << 2 & 0xCCu | val >> 2 & 0x33u;
    val = val << 1 & 0xAAu | val >> 1 & 0x55u;
    return val;
}

uint bit_interleave8(uint val1, uint val2) {
    return val1       & 0x00000001u
         | val1 <<  1 & 0x00000004u
         | val1 <<  2 & 0x00000010u
         | val1 <<  3 & 0x00000040u
         | val2 <<  1 & 0x00000002u
         | val2 <<  2 & 0x00000008u
         | val2 <<  3 & 0x00000020u
         | val2 <<  4 & 0x00000080u;
}
