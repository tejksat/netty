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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.hashcode.HashCodeResult.Collision;
import io.netty.util.collection.IntObjectMap;

import java.util.Collections;
import java.util.Comparator;

public class HashCodeClientHandler extends SimpleChannelInboundHandler<HashCodeResult> {
    private IntObjectMap<HashCodeRequest> requestMap;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final HashCodeResult msg) throws Exception {
        final HashCodeRequest request = requestMap.get(msg.requestId());
        if (request == null) {
            throw new Exception("result received with unknown request id: " + msg.requestId());
        }

        if (!msg.collisions().isEmpty()) {
            Collections.sort(msg.collisions(), new Comparator<Collision>() {
                @Override
                public int compare(Collision o1, Collision o2) {
                    return o1.collisionCount() < o2.collisionCount() ? -1 :
                           o1.collisionCount() > o2.collisionCount() ? 1 : 0;
                }
            });
            // array length, num samples, generator id, number collision instances,
            // min collision count, max collision count, mid-point collision coun
            StringBuilder b = new StringBuilder(11 * 6 + 7 + 1) // 6 integers, 7 commas, 1 byte
            .append(request.arrayLength())
            .append(',')
            .append(request.numSamples())
            .append(',')
            .append(msg.generatorId())
            .append(',')
            .append(msg.collisions().size())
            .append(',')
            .append((int) msg.collisions().get(0).collisionCount())
            .append(',')
            .append((int) msg.collisions().get(msg.collisions().size() - 1).collisionCount())
            .append(',')
            .append((int) msg.collisions().get(msg.collisions().size() >>> 2).collisionCount())
            .append('\n');
            System.out.println(b);
        }

        if (msg.lastPacket()) {
            requestMap.remove(msg.requestId());
            if (requestMap.isEmpty()) {
                // System.out.println("got a result for all requests...closing");
                ctx.close();
            }
        }
    }

    public void requestMap(IntObjectMap<HashCodeRequest> requestMap) {
        this.requestMap = requestMap;
    }
}
