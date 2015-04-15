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

import io.netty.util.internal.HashCodeGenerator.NativeTypeTranslator;

/**
 * Provides a means of pulling native types out of array type data structures.
 */
interface NativeTypeAccessor {
    int getInt(byte[] data, long i);
    byte getByte(byte[] data, int i);
    int getInt(CharSequence data, int i);
    int getIntAsBytes(CharSequence data, int i);
    char getChar(CharSequence data, int i);

    boolean isLittleEndian();
    long byteArrayBaseOffset();

    /**
     * Base type which provides "safe" access to build native types for {@link CharSequence} objects.
     */
    abstract static class AbstractNativeTypeAccessor implements NativeTypeAccessor {
        public int getIntAsBytes(CharSequence data, int i) {
            return data.charAt(i) | (data.charAt(i + 1) << 8) |
                   (data.charAt(i + 2) << 16) | (data.charAt(i + 3) << 24);
        }

        public int getInt(CharSequence data, int i) {
            return data.charAt(i) | (data.charAt(i + 1) << 16);
        }

        public char getChar(CharSequence data, int i) {
            return data.charAt(i);
        }
    }

    /**
     * Provides a level of indirection so a {@link NativeTypeAccessor} result can be intercepted by a
     * {@link NativeTypeTranslator}.
     */
    static final class ByteArrayAccessorDelegator implements NativeTypeAccessor {
        private final NativeTypeAccessor delegate;
        private final NativeTypeTranslator translator;

        public ByteArrayAccessorDelegator(NativeTypeAccessor delegate, NativeTypeTranslator translator) {
            this.delegate = delegate;
            this.translator = translator;
        }

        @Override
        public int getInt(byte[] data, long i) {
            return translator.translate(delegate.getInt(data, i));
        }

        @Override
        public byte getByte(byte[] data, int i) {
            return translator.translate(delegate.getByte(data, i));
        }

        @Override
        public boolean isLittleEndian() {
            return delegate.isLittleEndian();
        }

        @Override
        public long byteArrayBaseOffset() {
            return delegate.byteArrayBaseOffset();
        }

        @Override
        public int getInt(CharSequence data, int i) {
            return translator.translate(delegate.getInt(data, i));
        }

        @Override
        public int getIntAsBytes(CharSequence data, int i) {
            return translator.translate(delegate.getIntAsBytes(data, i));
        }

        @Override
        public char getChar(CharSequence data, int i) {
            return translator.translate(delegate.getChar(data, i));
        }
    }
}
