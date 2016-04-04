package com.juliasoft.beedeedee.factories;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class IntegrityCheckUniqueTable extends ResizingAndGarbageCollectedUniqueTable {

	protected static final int CHECKSUM_OFFSET = 5;
	protected static final int NODE_SIZE = 6;

	IntegrityCheckUniqueTable(int size, int cacheSize, ResizingAndGarbageCollectedFactoryImpl factory) {
		super(size, cacheSize, factory);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getNodeSize() {
		return NODE_SIZE;
	}

	private void checkNodeIntegrity(int id) {
		int value = nodeChecksum(id);
		if (value != ut[id * getNodeSize() + CHECKSUM_OFFSET]) {
			throw new CorruptedNodeException(id);
		}
	}

	private int nodeChecksum(int id) {
		ByteBuffer byteBuffer = ByteBuffer.allocate((getNodeSize() - 1) * 4);
		byteBuffer.putInt(ut[id * getNodeSize() + VAR_OFFSET]);
		byteBuffer.putInt(ut[id * getNodeSize() + LOW_OFFSET]);
		byteBuffer.putInt(ut[id * getNodeSize() + HIGH_OFFSET]);
		byteBuffer.putInt(ut[id * getNodeSize() + NEXT_OFFSET]);
		byteBuffer.putInt(ut[id * getNodeSize() + HASHCODEAUX_OFFSET]);
		byte[] byteArray = byteBuffer.array();
		Checksum checksum = new CRC32();
		checksum.update(byteArray, 0, byteArray.length);
		return (int) checksum.getValue();
	}

	@Override
	public int var(int id) {
		checkNodeIntegrity(id);
		return super.var(id);
	}

	@Override
	public int low(int id) {
		checkNodeIntegrity(id);
		return super.low(id);
	}

	@Override
	public int high(int id) {
		checkNodeIntegrity(id);
		return super.high(id);
	}

	@Override
	protected void setAt(int where, int varNumber, int lowNode, int highNode) {
		super.setAt(where, varNumber, lowNode, highNode);
		ut[where * getNodeSize() + CHECKSUM_OFFSET] = nodeChecksum(where);
	}
}
