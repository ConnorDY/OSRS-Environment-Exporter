// Adapted from https://www.pcg-random.org/download.html
uint pcg32_random_r(uint state_l, uint state_h, uint inc)
{
    uint oldstate_l = state_l;
    uint oldstate_h = state_h;
    // Advance internal state
    uint mul_l = 0x4c957f2dU;
    uint mul_h = 0x5851f42dU;

    state_l = mul_l * oldstate_l; // no multiply-with-carry in glsl??
    state_h = mul_h * oldstate_l + mul_l * oldstate_h;
    uint carry = (state_l >> 31) & (inc >> 31);
    state_l = state_l + (inc | 1u); // and no add-with-carry until 4.0??????
    state_h = state_h + carry;

    // Calculate output function (XSH RR), uses old state for max ILP
    uint xorshifted = (state_h >> 13) ^ (oldstate_l >> 27) ^ (state_h << 5);
    uint rot = state_h >> 27;
    return (xorshifted >> rot) | (xorshifted << ((-rot) & 31u));
}
