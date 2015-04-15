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

/**
 * Provides the ability to generate a hash code for array based objects.
 */
public interface HashCodeGenerator {
    /**
     * The hash code generator may extract native types out of the array based objects and this interface
     * may be used to translate the results before being used in the hash code algorithm.
     */
    interface NativeTypeTranslator {
        long translate(long value);
        int translate(int value);
        char translate(char value);
        byte translate(byte value);
    }

    /**
     * Generate a hash code from the [{@code startPos}, {@code endPos}) subsection of {@code bytes} using {@code seed}.
     */
    int hashCode(byte[] bytes, int startPos, int endPos, int seed);

    /**
     * Generate a hash code from the [{@code startPos}, {@code endPos}) subsection of {@code bytes} using {@code seed}.
     */
    int hashCodeAsBytes(CharSequence data, int offset, int len, int seed);
}
