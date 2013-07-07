/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Jeff Nelson, Cinchapi Software Collective
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cinchapi.concourse.server.engine;

import java.nio.ByteBuffer;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import org.cinchapi.common.annotate.DoNotInvoke;
import org.cinchapi.common.annotate.PackagePrivate;
import org.cinchapi.common.cache.ReferenceCache;
import org.cinchapi.common.io.ByteBuffers;
import org.cinchapi.common.io.Byteables;

/**
 * The association between a location and a {@link PrimaryKey}.
 * <p>
 * A Position is used in a {@link SearchIndex} to specify the precise location
 * of a term in a Record.
 * </p>
 * 
 * @author jnelson
 */
@Immutable
@PackagePrivate
final class Position implements Comparable<Position>, Storable {

	/**
	 * Return a Position based on the {@code key} and {@code marker}. The
	 * Position will have the storage properties of {@code key} and a unique
	 * timestamp.
	 * 
	 * @param key
	 * @param marker
	 * @return the Position
	 */
	public static Position fromPrimaryKeyAndMarker(PrimaryKey key, int marker) {
		if(key.isForStorage()) {
			key = PrimaryKey.forStorage(key.longValue()); // need to make a new
														// PrimaryKey to ensure
														// that timestamp is
														// unique
		}
		Object[] cacheKey = { key, marker, key.getTimestamp() };
		Position position = cache.get(cacheKey);
		if(position == null) {
			position = new Position(key, marker);
			cache.put(position, cacheKey);
		}
		return position;
	}

	/**
	 * Return the Position encoded in {@code buffer} so long as those bytes
	 * adhere to the format specified by the {@link #getBytes()} method. This
	 * method assumes that all the bytes in the {@code buffer} belong to the
	 * Value. In general, it is necessary to get the appropriate Value slice
	 * from the parent ByteBuffer using
	 * {@link ByteBuffers#slice(ByteBuffer, int, int)}.
	 * 
	 * @param buffer
	 * @return the Position
	 */
	public static Position fromByteBuffer(ByteBuffer buffer) {
		return Byteables.read(buffer, Position.class); // We are using
														// Byteables#read(ByteBuffer,
														// Class) instead of
														// calling
														// the constructor
														// directly
														// so as to take
														// advantage
														// of the automatic
														// reference caching
														// that is
														// provided in the
														// utility
														// class
	}

	/**
	 * The start position of the encoded PrimaryKey in {@link #bytes}.
	 */
	private static final int PK_POS = 0;

	/**
	 * The number of bytes used to encode the PrimaryKey in {@link #bytes}.
	 */
	private static final int PK_SIZE = PrimaryKey.SIZE;

	/**
	 * The start position of the encoded position in {@link #bytes}.
	 */
	private static final int POS_POS = PK_POS + PK_SIZE;

	/**
	 * The number of bytes used to encode the position in {@link #bytes}.
	 */
	private static final int POS_SIZE = Integer.SIZE / 8;

	/**
	 * The total number of bytes used to encoded each Position.
	 */
	@PackagePrivate
	static final int SIZE = PK_SIZE + POS_SIZE;

	/**
	 * A ReferenceCache is generated in {@link Byteables} for Positions read
	 * from ByteBuffers, so this cache is only for notForStorage Positions.
	 */
	private static final ReferenceCache<Position> cache = new ReferenceCache<Position>();

	/**
	 * <p>
	 * In order to optimize heap usage, we encode the PrimaryKey as a single
	 * ByteBuffer instead of storing each component as a member variable.
	 * </p>
	 * <p>
	 * To retrieve a component, we navigate to the appropriate position and
	 * convert the necessary bytes to the correct type, which is a cheap since
	 * binary conversion is trivial. Once a component is loaded onto the heap,
	 * it may be stored in an ReferenceCache for further future efficiency.
	 * </p>
	 * 
	 * The content conforms to the specification described by the
	 * {@link #getBytes()} method.
	 */
	private final ByteBuffer bytes;

	/**
	 * Construct an instance that represents an existing Position from a
	 * ByteBuffer. This constructor is public so as to comply with the
	 * {@link Byteable} interface. Calling this constructor directly is not
	 * recommend. Use {@link #fromByteBuffer(ByteBuffer)} instead to take
	 * advantage of reference caching.
	 * 
	 * @param bytes
	 */
	@DoNotInvoke
	public Position(ByteBuffer bytes) {
		this.bytes = bytes;
	}

	/**
	 * Construct a new instance.
	 * 
	 * @param key
	 * @param position
	 */
	private Position(PrimaryKey key, int position) {
		this.bytes = Positions.encodeAsByteBuffer(key, position);
	}

	@Override
	public int compareTo(Position o) {
		int comparison = getPrimaryKey().compareTo(o.getPrimaryKey(), true);
		return comparison == 0 ? Integer
				.compare(getPosition(), o.getPosition()) : comparison;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Position) {
			Position other = (Position) obj;
			return Objects.equals(getPrimaryKey(), other.getPrimaryKey())
					&& Objects.equals(getPosition(), other.getPosition());
		}
		return false;
	}

	@Override
	public ByteBuffer getBytes() {
		bytes.rewind();
		return bytes;
	}

	@Override
	public long getTimestamp() {
		return getPrimaryKey().getTimestamp();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getPrimaryKey(), getPosition());
	}

	@Override
	public boolean isForStorage() {
		return Storables.isForStorage(this);
	}

	@Override
	public boolean isNotForStorage() {
		return Storables.isNotForStorage(this);
	}

	@Override
	public int size() {
		return SIZE;
	}

	@Override
	public String toString() {
		return "Position " + getPosition() + " in PrimaryRecord "
				+ getPrimaryKey();
	}

	/**
	 * Determine if the comparison to {@code o} should be done temporally or
	 * {@code logically}.
	 * 
	 * @param o
	 * @param logically
	 *            if {@code true} the value based comparison occurs, otherwise
	 *            based on timestamp/equality
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 * @see {@link #compareTo(Position)}
	 * @see {@link Storables#compare(Storable, Storable)}
	 */
	@PackagePrivate
	int compareTo(Position o, boolean logically) {
		return logically ? compareTo(o) : Storables.compare(this, o);
	}

	/**
	 * Return the location marker that is associated with this Position.
	 * 
	 * @return the position
	 */
	@PackagePrivate
	int getPosition() {
		bytes.position(POS_POS);
		return bytes.getInt();
	}

	/**
	 * Return the PrimaryKey that is associated with this Position.
	 * 
	 * @return the PrimaryKey
	 */
	@PackagePrivate
	PrimaryKey getPrimaryKey() {
		return PrimaryKey.fromByteBuffer(ByteBuffers.slice(bytes, PK_POS,
				PK_SIZE));
	}

}