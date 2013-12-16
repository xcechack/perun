package cz.metacentrum.perun.dispatcher.scheduling.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.Pair;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.dispatcher.jms.DispatcherQueue;
import cz.metacentrum.perun.dispatcher.scheduling.SchedulingPool;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;
import cz.metacentrum.perun.taskslib.service.TaskManager;

// TODO: this shares a lot of code with engine.SchedulingPoolImpl - create abstract base with implementation specific indexes


@org.springframework.stereotype.Service("schedulingPool")
public class SchedulingPoolImpl implements SchedulingPool {

	private final static Logger log = LoggerFactory.getLogger(SchedulingPoolImpl.class);

	private Map<Integer, Pair<Task, DispatcherQueue>> tasksById = new ConcurrentHashMap<Integer, Pair<Task, DispatcherQueue>>();
	private Map<Pair<ExecService, Facility>, Task> tasksByServiceAndFacility = new ConcurrentHashMap<Pair<ExecService, Facility>, Task>();
    private Map<TaskStatus, List<Task>> pool = new EnumMap<TaskStatus, List<Task>>(TaskStatus.class);

    @Autowired
    private TaskManager taskManager;
    
    public SchedulingPoolImpl() {
    	for(TaskStatus status : TaskStatus.class.getEnumConstants()) {
    		pool.put(status, new ArrayList<Task>());
    	}
    }

    @Override
	public int getSize() {
		return tasksById.size();
	}

	@Override
	public int addToPool(Task task, DispatcherQueue dispatcherQueue) {
		// XXX needs to be synchronized, but on what?
		synchronized(tasksById) {
			if(!tasksById.containsKey(task.getId())) {
				log.debug("Adding task to pool " + task);
				if(null == task.getStatus()) {
					task.setStatus(TaskStatus.NONE);
				}
				tasksById.put(task.getId(), new Pair<Task, DispatcherQueue>(task, dispatcherQueue));
				tasksByServiceAndFacility.put(new Pair<ExecService, Facility>(task.getExecService(), task.getFacility()), task);
				pool.get(task.getStatus()).add(task);
			}
		}
		try {
			taskManager.scheduleNewTask(task, dispatcherQueue.getClientID());
		} catch (InternalErrorException e) {
			log.error("Error storing task " + task + " into database: " + e.getMessage());
		}
		return getSize();
	}

	@Override
	public Task getTaskById(int id) {
		Pair<Task, DispatcherQueue> entry = tasksById.get(id);
		if(entry == null) {
			return null;
		} else {
			return entry.getLeft();
		}
	}

	@Override
	public void removeTask(Task task) {
		Pair<Task, DispatcherQueue> val;
		synchronized(pool) {
			pool.get(task.getStatus()).remove(task);
			val = tasksById.remove(task.getId());
			tasksByServiceAndFacility.remove(new Pair(task.getExecService(), task.getFacility()));
		}
		taskManager.removeTask(task.getId(), val.getRight().getClientID());

	}

	@Override
	public Task getTask(ExecService execService, Facility facility) {
		return tasksByServiceAndFacility.get(new Pair<ExecService, Facility>(execService, facility));
	}

	@Override
	public DispatcherQueue getQueueForTask(Task task) throws InternalErrorException {
		Pair<Task, DispatcherQueue> entry = tasksById.get(task.getId());
		if(entry == null) {
			throw new InternalErrorException("no such task");
		}
		return entry.getRight();
	}

	@Override
	public void setTaskStatus(Task task, TaskStatus status) {
		TaskStatus old = task.getStatus();
		task.setStatus(status);
		// move task to the appropriate place
		if(!old.equals(status)) {
			pool.get(old).remove(task);
			pool.get(status).add(task);
		}
		taskManager.updateTask(task, tasksById.get(task.getId()).getRight().getClientID());
	}

	@Override
	public List<Task> getTasksForEngine(int clientID) {
		List<Task> result = new ArrayList<Task>();
		for(Pair<Task, DispatcherQueue> value : tasksById.values()) {
			if(clientID == value.getRight().getClientID()) {
				result.add(value.getLeft());
			}
		}
		return result;
	}

	@Override
	public List<Task> getWaitingTasks() {
		return new ArrayList<Task>(pool.get(TaskStatus.NONE));
	}

	@Override
	public List<Task> getDoneTasks() {
		return new ArrayList<Task>(pool.get(TaskStatus.DONE));
	}

	@Override
	public List<Task> getErrorTasks() {
		return new ArrayList<Task>(pool.get(TaskStatus.ERROR));
	}

	@Override
	public List<Task> getProcessingTasks() {
		return new ArrayList<Task>(pool.get(TaskStatus.PROCESSING));
	}

	@Override
	public List<Task> getPlannedTasks() {
		return new ArrayList<Task>(pool.get(TaskStatus.PLANNED));
	}

	@Override
	public void clear() {
		synchronized(tasksById) {
			tasksById.clear();
			tasksByServiceAndFacility.clear();
			for(TaskStatus status : TaskStatus.class.getEnumConstants()) {
					pool.get(status).clear();
			}
		}
		//taskManager.removeAllTasks();
	}

}
