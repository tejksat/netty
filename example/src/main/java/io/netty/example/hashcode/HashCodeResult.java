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

import java.util.ArrayList;
import java.util.List;

public final class HashCodeResult {
    private final List<Collision> collisions;
    private int requestId;
    private byte generatorId;
    private boolean lastPacket;

    public static enum HashCodeAlgorithm {
        JavaArrays(0), PlatformDependent(1);

        private final byte id;
        private HashCodeAlgorithm(int id) {
            this.id = (byte) id;
        }

        public byte id() {
            return id;
        }
    }

    public static final class Collision {
        private int hash;
        private char collisionCount;

        public Collision(int hash) {
            hash(hash);
            this.collisionCount = 2;
        }

        public int hash() {
            return hash;
        }

        public void hash(int hash) {
            this.hash = hash;
        }

        public char collisionCount() {
            return collisionCount;
        }

        public void collisionCount(char collisionCount) {
            this.collisionCount = collisionCount;
        }

        public void incrementCollisionCount() {
            ++this.collisionCount;
        }
    }

    public HashCodeResult(int numCollisions) {
        this.collisions = new ArrayList<Collision>(numCollisions);
    }

    public HashCodeResult(int numCollisions, int requestId, byte generatorId) {
        this.collisions = new ArrayList<Collision>(numCollisions);
        this.requestId = requestId;
        this.generatorId = generatorId;
    }

    public List<Collision> collisions() {
        return collisions;
    }

    public int requestId() {
        return requestId;
    }

    public void requestId(int requestId) {
        this.requestId = requestId;
    }

    public byte generatorId() {
        return generatorId;
    }

    public void generatorId(byte generatorId) {
        this.generatorId = generatorId;
    }

    public boolean lastPacket() {
        return lastPacket;
    }

    public void lastPacket(boolean lastPacket) {
        this.lastPacket = lastPacket;
    }
}
