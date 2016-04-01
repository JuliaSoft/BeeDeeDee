package com.juliasoft.beedeedee.factories;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class IntegrityCheckUniqueTable extends ResizingAndGarbageCollectedUniqueTable {

	protected static final int CHECKSUM_OFFSET = 5;
	public static final int NODE_SIZE = 6;

	IntegrityCheckUniqueTable(int size, int cacheSize, ResizingAndGarbageCollectedFactoryImpl factory) {
		super(size, cacheSize, factory);
		// TODO Auto-generated constructor stub
	}

	private void checksum(int id) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(NODE_SIZE - 1);
		byteBuffer.putInt(ut[id * NODE_SIZE + VAR_OFFSET]);
		byteBuffer.putInt(ut[id * NODE_SIZE + LOW_OFFSET]);
		byteBuffer.putInt(ut[id * NODE_SIZE + HIGH_OFFSET]);
		byteBuffer.putInt(ut[id * NODE_SIZE + NEXT_OFFSET]);
		byteBuffer.putInt(ut[id * NODE_SIZE + HASHCODEAUX_OFFSET]);
		byte[] byteArray = byteBuffer.array();
		Checksum checksum = new CRC32();
		checksum.update(byteArray, 0, byteArray.length);
		if (checksum.getValue() != ut[id * NODE_SIZE + CHECKSUM_OFFSET]) {
			throw new CorruptedNodeException(id);
		}
	}

	@Override
	public int var(int id) {
		checksum(id);
		return super.var(id);
	}

	@Override
	public int low(int id) {
		checksum(id);
		return super.low(id);
	}

	@Override
	public int high(int id) {
		checksum(id);
		return super.high(id);
	}
}
