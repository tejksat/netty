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
package io.netty.example.hashcode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class HashCodeRequestDecoder extends ByteToMessageDecoder {
    /**
     * 0 = waiting for new request id is next
     * 1 = arrayLength next
     * 2 = numSamples next
     */
    private byte state;
    private int id;
    private int arrayLength;
    private int numSamples;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        for (;;) {
            switch(state) {
                case 0:
                    if (in.readableBytes() < 4) {
                        return;
                    }
                    id = in.readInt();
                    state = 1;
                case 1:
                    if (in.readableBytes() < 4) {
                        return;
                    }
                    arrayLength = in.readInt();
                    state = 2;
                case 2:
                    if (in.readableBytes() < 4) {
                        return;
                    }
                    numSamples = in.readInt();
                    state = 0;
                    out.add(new HashCodeRequest(id, arrayLength, numSamples));
                    break;
                default:
                    throw new Error();
            }
        }
    }
}
