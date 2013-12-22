package com.juliasoft.utils.concurrent;

/**
 * A factory of <tt>java.lang.Runnable</tt>.
 *
 * @author <A HREF="mailto:fausto.spoto@univr.it">Fausto Spoto</A>
 */

public interface RunnableFactory {

	/**
	 * Yields a new instance of the runnable.
	 */

	public Runnable mk();
}