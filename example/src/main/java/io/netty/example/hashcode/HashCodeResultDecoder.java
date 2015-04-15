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
import io.netty.example.hashcode.HashCodeResult.Collision;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class HashCodeResultDecoder extends ByteToMessageDecoder {
    /**
     * 0 = collisions array length,
     * 1 = collision - hash,
     * 2 = collision - num collisions for has,
     * 3 = request id,
     * 4 = generator id,
     * 5 = last packet
     */
    private byte state;
    private int collisionCount;
    private Collision collision;
    private HashCodeResult result;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        for (;;) {
            switch (state) {
                case 0:
                    if (in.readableBytes() < 4) {
                        return;
                    }
                    collisionCount = in.readInt();
                    result = new HashCodeResult(collisionCount);
                    state = (byte) (collisionCount > 0 ? 1 : 3);
                    break;
                case 1:
                    if (in.readableBytes() < 4) {
                        return;
                    }
                    collision = new Collision(in.readInt());
                    result.collisions().add(collision);
                    state = 2;
                case 2:
                    if (in.readableBytes() < 2) {
                        return;
                    }
                    collision.collisionCount(in.readChar());
                    state = (byte) ((collisionCount > result.collisions().size()) ? 1 : 3);
                    break;
                case 3:
                    if (in.readableBytes() < 4) {
                        return;
                    }
                    result.requestId(in.readInt());
                    state = 4;
                case 4:
                    if (in.readableBytes() < 1) {
                        return;
                    }
                    result.generatorId(in.readByte());
                    state = 5;
                case 5:
                    if (in.readableBytes() < 1) {
                        return;
                    }
                    result.lastPacket(in.readBoolean());
                    out.add(result);
                    result = null;
                    state = 0;
                    break;
                default:
                    throw new Error();
            }
        }
    }
}
