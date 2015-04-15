/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.HashCodeGenerator.NativeTypeTranslator;

import java.util.Random;

import org.junit.Test;

public class PlatformDependentTest {

    @Test
    public void testEquals() {
        byte[] bytes1 = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
        byte[] bytes2 = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
        assertNotSame(bytes1, bytes2);
        assertTrue(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes1, 2, bytes1.length, bytes2, 2, bytes2.length));

        bytes1 = new byte[] {1, 2, 3, 4, 5, 6};
        bytes2 = new byte[] {1, 2, 3, 4, 5, 6, 7};
        assertNotSame(bytes1, bytes2);
        assertFalse(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes2, 0, 6, bytes1, 0, 6));

        bytes1 = new byte[] {1, 2, 3, 4};
        bytes2 = new byte[] {1, 2, 3, 5};
        assertFalse(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes1, 0, 3, bytes2, 0, 3));

        bytes1 = new byte[] {1, 2, 3, 4};
        bytes2 = new byte[] {1, 3, 3, 4};
        assertFalse(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes1, 2, bytes1.length, bytes2, 2, bytes2.length));

        bytes1 = new byte[0];
        bytes2 = new byte[0];
        assertNotSame(bytes1, bytes2);
        assertTrue(PlatformDependent.equals(bytes1, 0, 0, bytes2, 0, 0));

        bytes1 = new byte[100];
        bytes2 = new byte[100];
        for (int i = 0; i < 100; i++) {
            bytes1[i] = (byte) i;
            bytes2[i] = (byte) i;
        }
        assertTrue(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        bytes1[50] = 0;
        assertFalse(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes1, 51, bytes1.length, bytes2, 51, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes1, 0, 50, bytes2, 0, 50));

        bytes1 = new byte[]{1, 2, 3, 4, 5};
        bytes2 = new byte[]{3, 4, 5};
        assertFalse(PlatformDependent.equals(bytes1, 0, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes1, 2, bytes1.length, bytes2, 0, bytes2.length));
        assertTrue(PlatformDependent.equals(bytes2, 0, bytes2.length, bytes1, 2, bytes1.length));
    }

    @Test
    public void testHashCode() {
        final int bytes1Len = 256;
        final int bytes2Len = bytes1Len >> 1;
        final int bytes1Start = bytes2Len;
        final int bytes2Start = bytes2Len >> 1;
        int subSequenceLen = bytes2Len - bytes2Start;
        // We want to have a number that is divisible by 7 to ensure we hit all the
        // getlong/getint/getchar/direct lookup branches
        while (subSequenceLen % 7 != 0) {
            --subSequenceLen;
        }
        byte[] bytes1 = new byte[bytes1Len];
        byte[] bytes2 = new byte[bytes2Len];
        Random r = new Random();
        r.nextBytes(bytes1);
        System.arraycopy(bytes1, bytes1Start, bytes2, bytes2Start, subSequenceLen);
        assertNotSame(bytes1, bytes2);

        final int seed = r.nextInt();
        // Test that two separate arrays with the same value for a given range yield the same hash code.
        assertEquals(PlatformDependent.hashCode(bytes1, bytes1Start, bytes1Start + subSequenceLen, seed),
                PlatformDependent.hashCode(bytes2, bytes2Start, bytes2Start + subSequenceLen, seed));

        // Test that the "safe" fall back and the potentially optimized hash code algorithms yield the same hash code.
        assertEquals(PlatformDependent.safeHashCode(bytes1, bytes1Start, bytes1Start + subSequenceLen, seed),
                PlatformDependent.hashCode(bytes2, bytes2Start, bytes2Start + subSequenceLen, seed));
    }

    /**
     * Values are all generated from running the
     * <a href="https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp">C++ reference code</a>.
     */
    @Test
    public void testMurmurKnownHashes() {
        final int seed = 0;
        assertEquals(0, PlatformDependent.hashCode(new byte[0], seed));
        assertEquals(-809654831, PlatformDependent.hashCode(new byte[] {'k'}, seed));
        assertEquals(-1587029005, PlatformDependent.hashCode(new String("hell").getBytes(CharsetUtil.US_ASCII), seed));
        assertEquals(613153351, PlatformDependent.hashCode(
                new String("hello").getBytes(CharsetUtil.US_ASCII), seed));
        assertEquals(1027717500, PlatformDependent.hashCode(
                new String("http://www.google.com/").getBytes(CharsetUtil.US_ASCII), seed));
        assertEquals(776992547, PlatformDependent.hashCode(
                new String("The quick brown fox jumps over the lazy dog").getBytes(CharsetUtil.US_ASCII), seed));
        assertEquals(1313074686, PlatformDependent.hashCode(
                new String("fsafdffsafkljfl;iewn;cl poiun[c0   2im;lkm;lc HFOWPOM").getBytes(CharsetUtil.UTF_8), seed));
    }

    @Test
    public void testMurmurDifferntTypesSameResults() {
        HashCodeGenerator hasher = PlatformDependent.newHashCodeGenerator(new NativeTypeTranslator() {
            @Override
            public long translate(long value) {
                return value;
            }

            @Override
            public int translate(int value) {
                return value;
            }

            @Override
            public char translate(char value) {
                return value;
            }

            @Override
            public byte translate(byte value) {
                return value;
            }
        });

        Random r = new Random();
        final int seed = r.nextInt();
        final int min = 4;
        final int max = 2000;
        int len = r.nextInt((max - min) + 1) + min;
        // We need length to be divisible by 4
        while (len % 4 != 0) {
            ++len;
        }
        // We want to test all permutations of bytes ending on an even int boundary,
        // and not ending on an even int boundary
        runHasherEqualityTest(hasher, seed, len, r);
        runHasherEqualityTest(hasher, seed, len + 1, r);
        runHasherEqualityTest(hasher, seed, len + 2, r);
        runHasherEqualityTest(hasher, seed, len + 3, r);
    }

    private static void runHasherEqualityTest(HashCodeGenerator hasher, int seed, int len, Random r) {
        byte[] bytes = new byte[len];
        char[] charsAsBytes = new char[bytes.length];
        r.nextBytes(bytes);

        for (int i = 0; i < bytes.length; ++i) {
            charsAsBytes[i] = (char) (bytes[i] & 0xFF);
        }

        String a = new String(charsAsBytes);
        assertEquals(hasher.hashCodeAsBytes(a, 0, a.length(), seed), hasher.hashCode(bytes, 0, bytes.length, seed));
    }
}
