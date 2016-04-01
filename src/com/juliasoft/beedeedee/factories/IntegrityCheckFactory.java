package com.juliasoft.beedeedee.factories;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * A factory with integrity check of BDD data.
 */
public class IntegrityCheckFactory extends ResizingAndGarbageCollectedFactoryImpl {

	public IntegrityCheckFactory(int utSize, int cacheSize, boolean onlineCheck) {
		this(utSize, cacheSize, DEFAULT_NUMBER_OF_PREALLOCATED_VARS, onlineCheck);
	}

	public IntegrityCheckFactory(int utSize, int cacheSize, int numberOfPreallocatedVars, boolean onlineCheck) {
		super(utSize, cacheSize, numberOfPreallocatedVars);
		if (onlineCheck) { // substitute unique table
			ut = new IntegrityCheckUniqueTable(utSize, cacheSize, this);
			// insert 0 and 1
			ZERO = ut.get(Integer.MAX_VALUE - 1, -1, -1);
			ONE = ut.get(Integer.MAX_VALUE, -1, -1);

			// insert lower variables
			for (int var = 0; var < NUMBER_OF_PREALLOCATED_VARS; var++)
				vars[var] = ut.get(var, ZERO, ONE);

			// and their negation
			for (int var = 0; var < NUMBER_OF_PREALLOCATED_VARS; var++)
				notVars[var] = ut.get(var, ONE, ZERO);
		}
	}

	/**
	 * Computes a checksum of the BDD data present in this factory.
	 * 
	 * @return the checksum
	 */
	public long computeChecksum() {
		long crc32h = computeCRC32(ut.H);
		long crc32ut = computeCRC32(ut.ut);
		return crc32h + crc32ut;
	}

	private long computeCRC32(int[] a) {
		// convert int[] to byte[]
		ByteBuffer byteBuffer = ByteBuffer.allocate(a.length * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(a);
		byte[] byteArray = byteBuffer.array();

		Checksum checksum = new CRC32();
		checksum.update(byteArray, 0, byteArray.length);
		return checksum.getValue();
	}

}
