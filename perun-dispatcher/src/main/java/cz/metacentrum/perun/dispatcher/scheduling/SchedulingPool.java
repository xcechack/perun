package cz.metacentrum.perun.dispatcher.scheduling;

import java.util.List;

import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.Task;

/**
 * 
 * @author Michal Karm Babacek JavaDoc coming soon...
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
     * @return
     */
    int addToPool(Task task);

	Task getTaskById(int id);

	void removeTask(Task task);

	List<Task> getWaitingTasks();

	Task getTask(ExecService execService, Facility facility);

}
