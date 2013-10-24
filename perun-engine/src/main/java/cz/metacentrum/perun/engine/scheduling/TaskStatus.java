package cz.metacentrum.perun.engine.scheduling;

import java.util.List;

import cz.metacentrum.perun.core.api.Destination;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.TaskResult;

public interface TaskStatus {

	public enum TaskDestinationStatus {
		WAITING,
		PROCESSING,
		DONE,
		ERROR
	}

	List<Destination> getWaitingDestinations();

	TaskDestinationStatus getDestinationStatus(Destination destination) throws InternalErrorException;
	
	void setDestinationStatus(Destination destination, TaskDestinationStatus status) throws InternalErrorException;

	void setResult(TaskResult result);

	boolean isTaskFinished();

	cz.metacentrum.perun.taskslib.model.Task.TaskStatus getTaskStatus();

}
