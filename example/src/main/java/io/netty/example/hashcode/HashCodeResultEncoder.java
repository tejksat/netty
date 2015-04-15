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
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

public class HashCodeResultEncoder extends MessageToByteEncoder<HashCodeResult> {
    @Override
    protected void encode(ChannelHandlerContext ctx, HashCodeResult msg, ByteBuf out) throws Exception {
        final List<Collision> collisions = msg.collisions();
        out.writeInt(collisions.size());
        for (Collision c : collisions) {
            out.writeInt(c.hash());
            out.writeChar(c.collisionCount());
        }
        out.writeInt(msg.requestId());
        out.writeByte(msg.generatorId());
        out.writeBoolean(msg.lastPacket());
    }
}
