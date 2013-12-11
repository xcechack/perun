package cz.metacentrum.perun.dispatcher.scheduling;

import java.util.List;

import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.dispatcher.jms.DispatcherQueue;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;

/**
 * 
 * @author Michal Voců
 * 
 * Contains:
 *   - database of Tasks and their states
 *   - mapping of Tasks to engines (dispatcherQueue)
 *   
 */
public interface SchedulingPool {

    /**
     * Size
     * 
     * @return current pool size
     */
    int getSize();

    /**
     * Add Task to the waiting list.
     * 
     * @param task
     * @param dispatcherQueue 
     * @return
     */
    int addToPool(Task task, DispatcherQueue dispatcherQueue);

	Task getTaskById(int id);

	void removeTask(Task task);

	List<Task> getWaitingTasks();

	Task getTask(ExecService execService, Facility facility);

	DispatcherQueue getQueueForTask(Task task) throws InternalErrorException;

	void setTaskStatus(Task task, TaskStatus status);

	List<Task> getTasksForEngine(int clientID);

}