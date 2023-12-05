// Adapted by Joseph Lunderville from code found on 2023-12-03 at:
//
// https://prng.di.unimi.it/xoshiro256starstar.c

// Written in 2018 by David Blackman and Sebastiano Vigna (vigna@acm.org)

// To the extent possible under law, the author has dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.

// See <http://creativecommons.org/publicdomain/zero/1.0/>.

package synth.algorithms.rng;

import java.util.Random;

/**
 * This is xoshiro256** 1.0, one of our all-purpose, rock-solid
 * generators. It has excellent (sub-ns) speed, a state (256 bits) that is
 * large enough for any parallel application, and it passes all tests we
 * are aware of.
 * 
 * For generating just floating-point numbers, xoshiro256+ is even faster.
 * 
 * The state must be seeded so that it is not everywhere zero. If you have
 * a 64-bit seed, we suggest to seed a splitmix64 generator and use its
 * output to fill s.
 */
public class Xoshiro256SS implements Cloneable {
    private static final long JUMP[] = { 0x180ec6d33cfd0abaL, 0xd5a61266f0c9392cL, 0xa9582618e03fc9aaL,
            0x39abdc4529b1661cL };
    private static final long LONG_JUMP[] = { 0x76e15d3efefdcbbfL, 0xc5004e441c522fb3L, 0x77710069854ee241L,
            0x39109bb02acbe635L };

    private long s0, s1, s2, s3;
    private boolean jumpClone, longJumpClone;

    public Xoshiro256SS() {
        reset();
    }

    public Xoshiro256SS(long seed) {
        reset(seed);
    }

    public Xoshiro256SS(java.util.Random r) {
        reset(r);
    }

    public Xoshiro256SS(Xoshiro256SS other) {
        this.s0 = other.s0;
        this.s1 = other.s1;
        this.s2 = other.s2;
        this.s3 = other.s3;
    }

    public void reset() {
        reset(new Random());
    }

    public void reset(long seed) {
        reset(new Random(seed));
    }

    public void reset(java.util.Random r) {
        s0 = r.nextLong();
        s1 = r.nextLong();
        s2 = r.nextLong();
        s3 = r.nextLong();
    }

    public long nextLong() {
        final long result = rotl(s1 * 5, 7) * 9;
        final long t = s1 << 17;

        s2 ^= s0;
        s3 ^= s1;
        s1 ^= s2;
        s0 ^= s3;

        s2 ^= t;

        s3 = rotl(s3, 45);

        return result;
    }

    public int nextInt() {
        return (int) (this.nextLong() >> 32);
    }

    private static int fillBitsRight(int x) {
        int y = x;
        y |= y >>> 1;
        y |= y >>> 2;
        y |= y >>> 4;
        y |= y >>> 8;
        y |= y >>> 16;
        return y;
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        } else {
            int mask = fillBitsRight(bound - 1);
            if ((bound & mask) == 0) {
                return (int) (mask & nextInt());
            } else {
                int r = nextInt() & mask;
                for (int i = 0; r >= bound; ++i) {
                    if (i >= 100) {
                        // statistically speaking shouldn't happen, but hey let's make it easy to prove
                        // for realsie
                        return (int) (nextLong() % bound);
                    }
                    r = nextInt() & mask;
                }
                return r;
            }
        }
    }

    public boolean nextBoolean() {
        return this.nextLong() >= 0;
    }

    public float nextFloat() {
        return (float) (nextLong() >>> (64 - 24)) / 1.6777216E7F;
    }

    public double nextDouble() {
        return (double) (nextLong() >>> (64 - 53)) * 1.1102230246251565E-16;
    }

    public Xoshiro256SS nextSubsequence() {
        if (jumpClone) {
            throw new UnsupportedOperationException(
                    "This RNG was cloned from a parent sequence, taking more subsequences from it "
                            + "will cause collisions with the parent stream.");
        }
        var cloned = new Xoshiro256SS(this);
        cloned.jumpClone = true;
        cloned.longJumpClone = true;
        // Jump past the subsequence we just spawned
        jump();
        return cloned;
    }

    public Xoshiro256SS nextLongSubsequence() {
        if (longJumpClone) {
            throw new UnsupportedOperationException(
                    "This RNG was cloned from a parent sequence, taking more subsequences from it "
                            + "will cause collisions with the parent stream.");
        }
        var cloned = new Xoshiro256SS(this);
        cloned.longJumpClone = true;
        // Jump past the subsequence we just spawned (and its subsequences)
        longJump();
        return cloned;
    }

    /**
     * This is the jump function for the generator. It is equivalent
     * to 2^128 calls to next(); it can be used to generate 2^128
     * non-overlapping subsequences for parallel computations.
     */
    protected void jump() {
        long s0T = 0, s1T = 0, s2T = 0, s3T = 0;

        for (int i = 0; i < JUMP.length; i++) {
            for (int b = 0; b < 64; b++) {
                if ((JUMP[i] & (1 << b)) != 0) {
                    s0T ^= s0;
                    s1T ^= s1;
                    s2T ^= s2;
                    s3T ^= s3;
                }
                nextLong();
            }
        }

        s0 = s0T;
        s1 = s1T;
        s2 = s2T;
        s3 = s3T;
    }

    /**
     * This is the long-jump function for the generator. It is equivalent to
     * 2^192 calls to next(); it can be used to generate 2^64 starting points,
     * from each of which jump() will generate 2^64 non-overlapping
     * subsequences for parallel distributed computations.
     */
    protected void longJump() {
        long s0T = 0, s1T = 0, s2T = 0, s3T = 0;

        for (int i = 0; i < LONG_JUMP.length; i++) {
            for (int b = 0; b < 64; b++) {
                if ((LONG_JUMP[i] & (1 << b)) != 0) {
                    s0T ^= s0;
                    s1T ^= s1;
                    s2T ^= s2;
                    s3T ^= s3;
                }
                nextLong();
            }
        }

        s0 = s0T;
        s1 = s1T;
        s2 = s2T;
        s3 = s3T;
    }

    private static long rotl(long x, int k) {
        return (x << k) | (x >>> (64 - k));
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new Xoshiro256SS(this);
    }
}
