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

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * <a href="https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp">MurmurHash3.cpp</a>.
 */
final class Murmur3 implements HashCodeGenerator {
    private static final int MurmurHash3_C1 = 0xcc9e2d51;
    private static final int MurmurHash3_C2 = 0x1b873593;
    private static final byte MurmurHash3_ROLT1 = 15;
    private static final byte MurmurHash3_ROLT2 = 13;
    private final boolean littleEndian;
    private final NativeTypeAccessor accessor;

    Murmur3(NativeTypeAccessor accessor) {
        this.accessor = checkNotNull(accessor, "accessor");
        this.littleEndian = accessor.isLittleEndian();
    }

    // TODO: add char[] hashCode method?

    @Override
    public int hashCodeAsBytes(CharSequence data, int offset, int len, int seed) {
        int h1 = seed;
        //----------
        // body
        final int numIntsOffset = (len >>> 2) << 2;
        final int end = offset + numIntsOffset;
        for (int i = offset; i < end; i += 4) {
            int k1 = accessor.getIntAsBytes(data, i);
            k1 *= MurmurHash3_C1;
            k1 = Integer.rotateLeft(k1, MurmurHash3_ROLT1);
            k1 *= MurmurHash3_C2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, MurmurHash3_ROLT2);
            h1 = (h1 * 5) + 0xe6546b64;
        }

        //----------
        // tail
        offset += numIntsOffset;
        if (littleEndian) {
            int k1 = 0;
            switch (len & 3) {
                case 3: k1 = ((byte) accessor.getChar(data, offset + 2)) << 16;
                case 2: k1 ^= ((byte) accessor.getChar(data, offset + 1)) << 8;
                case 1: k1 ^= (byte) accessor.getChar(data, offset);
                h1 ^= MurmurHash3TailCommon(k1);
                default:
                    break;
            }
        } else {
            int k1 = 0;
            switch (len & 3) {
                case 3: k1 = ((byte) accessor.getChar(data, offset + 2)) << 8;
                case 2: k1 ^= ((byte) accessor.getChar(data, offset + 1)) << 16;
                case 1: k1 ^= ((byte) accessor.getChar(data, offset)) << 24;
                h1 ^= MurmurHash3TailCommon(k1);
                default:
                    break;
            }
        }

        //----------
        // finalization
        h1 ^= len;
        return (int) MurmurHash3Fmix(h1);
    }

   /**
    * Designed to yield identical results as
    * <a href="https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp">MurmurHash3_x86_32</a>.
    * A version was implemented to use longs for unsigned 32-bit operations but performance suffered by about 2x.
    */
   @Override
   public int hashCode(byte[] data, int offset, int len, int seed) {
         int h1 = seed;
         //----------
         // body
         final int numIntsOffset = (len >>> 2) << 2;
         final long adjustedOffset = offset + accessor.byteArrayBaseOffset();
         final long end = adjustedOffset + numIntsOffset;
         for (long i = adjustedOffset; i < end; i += 4) {
             int k1 = accessor.getInt(data, i);
             k1 *= MurmurHash3_C1;
             k1 = Integer.rotateLeft(k1, MurmurHash3_ROLT1);
             k1 *= MurmurHash3_C2;

             h1 ^= k1;
             h1 = Integer.rotateLeft(h1, MurmurHash3_ROLT2);
             h1 = (h1 * 5) + 0xe6546b64;
         }

         //----------
         // tail
         offset += numIntsOffset;
         if (littleEndian) {
             int k1 = 0;
             switch (len & 3) {
                 case 3: k1 = accessor.getByte(data, offset + 2) << 16;
                 case 2: k1 ^= accessor.getByte(data, offset + 1) << 8;
                 case 1: k1 ^= accessor.getByte(data, offset);
                 h1 ^= MurmurHash3TailCommon(k1);
                 default:
                     break;
             }
         } else {
             int k1 = 0;
             switch (len & 3) {
                 case 3: k1 = accessor.getByte(data, offset + 2) << 8;
                 case 2: k1 ^= accessor.getByte(data, offset + 1) << 16;
                 case 1: k1 ^= accessor.getByte(data, offset) << 24;
                 h1 ^= MurmurHash3TailCommon(k1);
                 default:
                     break;
             }
         }

         //----------
         // finalization
         h1 ^= len;
         return (int) MurmurHash3Fmix(h1);
     }

     private static int MurmurHash3Fmix(int h) {
         h ^= h >>> 16;
         h *= 0x85ebca6b;
         h ^= h >>> 13;
         h *= 0xc2b2ae35;
         h ^= h >>> 16;
         return h;
     }

     private static int MurmurHash3TailCommon(int k1) {
         k1 *= MurmurHash3_C1;
         k1 = Integer.rotateLeft(k1, MurmurHash3_ROLT1);
         return k1 * MurmurHash3_C2;
     }
}
