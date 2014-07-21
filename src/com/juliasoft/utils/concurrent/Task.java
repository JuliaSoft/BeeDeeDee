/* 
  Copyright 2014 Julia s.r.l.
    
  This file is part of BeeDeeDee.

  BeeDeeDee is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  BeeDeeDee is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with BeeDeeDee.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.juliasoft.utils.concurrent;

import static checkers.nullness.support.NullnessAssertions.assertNonNull;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A task. It represents a computation that is not yet completed.
 *
 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
 */

public class Task<T> {

	/**
	 * The future of the computation.
	 */

	private volatile Future<T> future;

	/**
	 * The result of the computation, when available.
	 */

	private volatile T result;

	/**
	 * Builds a task with the given future.
	 *
	 * @param future the future
	 */

	protected Task(Future<T> future) {
		assertNonNull(future);
		this.future = future;
	}

	/**
	 * Yields a task that yields the given result. This means that
	 * the task is not really running, but has already completed when
	 * it is created.
	 *
	 * @param result the result
	 */

	public Task(final T result) {
		this(new Future<T>() {

			@Override
			public boolean cancel(boolean arg0) {
				return false;
			}

			@Override
			public T get() {
				return result;
			}

			@Override
			public T get(long arg0, TimeUnit arg1) {
				return result;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return true;
			}
		});
	}

	/**
	 * Yields the result of the task. Blocks if the result is
	 * not yet available. Any exception thrown during the
	 * computation of the result is wrapped inside a
	 * runtime exception.
	 *
	 * @return the result of the task
	 */

	public T get() {
		if (result != null)
			return result;

		synchronized(this) {
			try {
				if (result == null) {
					result = future.get();
					future = null;
				}

				return result;
			}
			// we transform any exception in a runtime exception
			// so that we do not have to put a throws clause
			// in a lot of methods
			catch (InterruptedException e) {
				Throwable cause = e.getCause();
				throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
			}
			catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			}
		}
	}
}