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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.juliasoft.julia.checkers.nullness.assertions.NullnessAssertions.*;

/**
 * An utility class to run tasks in parallel or asynchronously.
 *
 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
 */

public abstract class Executors {

	/**
	 * A thread-pool of executors.
	 */
	
	private final static ExecutorService exec = java.util.concurrent.Executors.newCachedThreadPool();

	/**
	 * The number of cores available on the run-time system.
	 */

	private final static int cpus = Runtime.getRuntime().availableProcessors();

	/**
	 * Submits the given task as an asynchronous task.
	 *
	 * @param <T> the type of the result of the task
	 * @param task the task
	 * @return the future of the task
	 */

	public static <T> Task<T> submit(Callable<T> task) {
		assertNonNull(task);
		return new Task<T>(exec.submit(new WrapperCallable<T>(task)));
	}

	/**
	 * A wrapper of a callable. This allows the garbage collector to claim back
	 * all resources allocated for the wrapped callable.
	 *
	 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
	 *
	 * @param <T> the type of the result of the callable
	 */

	private static class WrapperCallable<T> implements Callable<T> {

		/**
		 * The wrapped callable.
		 */

		private Callable<T> callable;

		/**
		 * Builds the wrapper.
		 *
		 * @param callable the wrapped callable
		 */

		private WrapperCallable(Callable<T> callable) {
			this.callable = callable;
		}

		@Override
		public T call() throws Exception {
			Callable<T> callable = this.callable;

			if (callable != null) {
				T result = callable.call();

				// we unlink the callable and everything that might be reachable from it
				this.callable = null;

				return result;
			}

			return null;
		}
		
	}

	/**
	 * Runs as many instances of the given {@code task} as there
	 * are available processors on this system. It blocks until all
	 * instances have finished.
	 *
	 * @param task the task
	 */

	public static void parallelise(Runnable task) {
		Future<?>[] future = new Future<?>[cpus];

		for (int i = 0; i < cpus; i++)
			future[i] = exec.submit(task);

		for (int i = 0; i < cpus; i++)
			try {
				future[i].get();
			}
			// we transform any exception in a runtime exception
			// so that we do not have to express a throws clause
			// in a lot of methods
			catch (InterruptedException e) {
				e.printStackTrace();
				Throwable cause = e.getCause();
				throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
			}
			catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException(e.getCause());
			}
	}

	/**
	 * Runs the given tasks in parallel. It blocks until they have all completed.
	 *
	 * @param tasks the tasks
	 */

	public static void parallelise(Runnable... tasks) {
		assertNonNull(tasks);
		Future<?>[] future = new Future<?>[tasks.length];

		for (int i = 0; i < future.length; i++)
			future[i] = exec.submit(tasks[i]);

		for (int i = 0; i < future.length; i++)
			try {
				future[i].get();
			}
			// we transform any exception in a runtime exception
			// so that we do not have to express a throws clause
			// in a lot of methods
			catch (InterruptedException e) {
				e.printStackTrace();
				Throwable cause = e.getCause();
				throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
			}
			catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException(e.getCause());
			}
	}

	/**
	 * Runs the given tasks in parallel. It blocks until they have all completed.
	 *
	 * @param tasks the tasks
	 */

	public static void parallelise(Collection<? extends Runnable> tasks) {
		assertNonNull(tasks);
		Future<?>[] futures = new Future<?>[tasks.size()];

		int i = 0;
		for (Runnable task: tasks)
			futures[i++] = exec.submit(task);

		for (Future<?> future: futures)
			try {
				future.get();
			}
			// we transform any exception in a runtime exception
			// so that we do not have to express a throws clause
			// in a lot of methods
			catch (InterruptedException e) {
				e.printStackTrace();
				Throwable cause = e.getCause();
				throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
			}
			catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException(e.getCause());
			}
	}

	/**
	 * Creates with the given factory and runs as many tasks as there
	 * are available processors on this system. It blocks until all
	 * instances have finished.
	 *
	 * @param factory the factory
	 */

	public static void parallelise(RunnableFactory factory) {
		assertNonNull(factory);
		Runnable[] tasks = new Runnable[cpus];

		for (int i = 0; i < cpus; i++)
			tasks[i] = factory.mk();

		parallelise(tasks);
	}

	/**
	 * Shutdowns the executors.
	 */

	public static void shutdown() {
		exec.shutdown();
	}
}