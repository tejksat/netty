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
import io.netty.example.hashcode.HashCodeResult.HashCodeAlgorithm;
import io.netty.util.collection.ByteObjectHashMap;
import io.netty.util.collection.ByteObjectMap;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.PlatformDependent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Accepts {@link HashCodeRequest} objects, computes the hash code with multiple algorithms,
 * tracks collisions, and reports back the results to the requester.
 */
public class HashCodeServerHandler extends SimpleChannelInboundHandler<HashCodeRequest> {
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    /**
     * ThreadLocalRandom.current() is jdk7+. So just use this to be safe.
     */
    private static final ThreadLocal<Random> localRandom = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    private interface HashCodeGenerator {
        int generateHashCode(byte[] value);

        byte id();
    }

    private static final HashCodeGenerator ARRAYS_GENERATOR = new HashCodeGenerator() {
        @Override
        public int generateHashCode(byte[] value) {
            return Arrays.hashCode(value);
        }

        public byte id() {
            return HashCodeAlgorithm.JavaArrays.id();
        }
    };

    private static final HashCodeGenerator PLATFORM_GENERATOR = new HashCodeGenerator() {
        @Override
        public int generateHashCode(byte[] value) {
            return PlatformDependent.hashCode(value, 0, value.length, 0);
        }

        public byte id() {
            return HashCodeAlgorithm.PlatformDependent.id();
        }
    };

    private final class HashCodeGeneratorTask implements Runnable {
        private final ChannelHandlerContext ctx;
        private final HashCodeRequest request;
        private byte[][] bytes;
        /**
         * Key = hash code, value = ByteObjectMap<Key = hash code generator id, Value = index into bytes>
         */
        private IntObjectMap<ByteObjectMap<List<Integer>>> collisionMap;

        public HashCodeGeneratorTask(ChannelHandlerContext ctx, HashCodeRequest request) {
            this.ctx = ctx;
            this.request = request;
        }

        @Override
        public void run() {
            this.bytes = new byte[request.numSamples()][];
            collisionMap = new IntObjectHashMap<ByteObjectMap<List<Integer>>>(request.numSamples());
            HashCodeResult arraysResult = new HashCodeResult(1, request.id(), ARRAYS_GENERATOR.id());
            HashCodeResult platformResult = new HashCodeResult(1, request.id(), PLATFORM_GENERATOR.id());
            for (int i = 0; i < request.numSamples(); ++i) {
                byte[] currBytes = bytes[i] = new byte[request.arrayLength()];
                localRandom.get().nextBytes(currBytes);
                findCollisions(currBytes, i, ARRAYS_GENERATOR, arraysResult);
                findCollisions(currBytes, i, PLATFORM_GENERATOR, platformResult);
            }
            ctx.write(arraysResult);
            platformResult.lastPacket(true);
            ctx.writeAndFlush(platformResult);
        }

        private void findCollisions(byte[] bytes, int bytesIndex, HashCodeGenerator generator, HashCodeResult result) {
            int hash = generator.generateHashCode(bytes);
            ByteObjectMap<List<Integer>> collisionGeneratorMap = collisionMap.get(hash);
            if (collisionGeneratorMap == null) {
                collisionGeneratorMap = new ByteObjectHashMap<List<Integer>>(1);
                collisionMap.put(hash, collisionGeneratorMap);
                List<Integer> collisionList = new ArrayList<Integer>(1);
                collisionList.add(bytesIndex);
                collisionGeneratorMap.put(generator.id(), collisionList);
                return;
            }
            List<Integer> collisionList = collisionGeneratorMap.get(generator.id());
            if (collisionList == null) {
                collisionList = new ArrayList<Integer>(1);
                collisionList.add(bytesIndex);
                collisionGeneratorMap.put(generator.id(), collisionList);
                return;
            }
            for (Integer previousBytesIndex : collisionList) {
                byte[] testBytes = this.bytes[previousBytesIndex];
                if (PlatformDependent.equals(testBytes, 0, testBytes.length, bytes, 0, bytes.length)) {
                    return;
                }
            }
            for (Collision collision : result.collisions()) {
                if (collision.hash() == hash) {
                    collision.incrementCollisionCount();
                    return;
                }
            }
            result.collisions().add(new Collision(hash));
        }
    };

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        System.err.println("Cancelling all tasks!");
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HashCodeRequest msg) throws Exception {
        executorService.execute(new HashCodeGeneratorTask(ctx, msg));
    }
}
