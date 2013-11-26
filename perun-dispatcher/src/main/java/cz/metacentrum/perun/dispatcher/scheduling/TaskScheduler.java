package cz.metacentrum.perun.dispatcher.scheduling;


import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;

public interface TaskScheduler {

	/**
	 * Process waiting jobs in the pool, sort them to satisfy dependencies
	 * and sent them out to engine(s) for execution.
	 * 
	 * @throws InternalErrorException
	 */
	void processPool() throws InternalErrorException;

    int getPoolSize();

	void closeTasksForEngine(int clientID);

	void onTaskComplete(int parseInt, int clientID, String string);

}
