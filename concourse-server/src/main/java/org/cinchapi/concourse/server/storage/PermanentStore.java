/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013-2015 Jeff Nelson, Cinchapi Software Collective
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
package org.cinchapi.concourse.server.storage;

import org.cinchapi.concourse.server.storage.temp.Write;

/**
 * A {@link Store} that accepts {@link Write} objects that are transported from
 * a {@link Limbo}. This service relies on rich indexing to offer optimal
 * read performance.
 * 
 * @author jnelson
 */
public interface PermanentStore extends Store {

    /**
     * Process and store {@code write}.
     * 
     * @param write
     */
    public void accept(Write write);

    /**
     * Process and store {@code write} while obeying the directive to
     * {@code sync} or not. A sync guarantees that the write is durably
     * persisted.
     * 
     * @param write
     * @param sync
     */
    public void accept(Write write, boolean sync);

    /**
     * Force the store to sync all of its writes to disk to guarantee that they
     * are durably persisted. Generally, this method will "fsync" pending writes
     * that {@link #accept(Write, boolean) were not synced when accepted}. For
     * example, this is a way to enable <em>group sync</em> functionality.
     */
    public void sync();
}
