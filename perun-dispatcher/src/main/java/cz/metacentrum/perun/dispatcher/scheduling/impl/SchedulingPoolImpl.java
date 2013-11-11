package cz.metacentrum.perun.dispatcher.scheduling.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.Pair;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.dispatcher.jms.DispatcherQueue;
import cz.metacentrum.perun.dispatcher.scheduling.SchedulingPool;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;

// TODO: this shares a lot of code with engine.SchedulingPoolImpl - create abstract base with implementation specific indexes

@org.springframework.stereotype.Service("schedulingPool")
public class SchedulingPoolImpl implements SchedulingPool {

	private final static Logger log = LoggerFactory.getLogger(SchedulingPoolImpl.class);

	private Map<Integer, Pair<Task, DispatcherQueue>> tasksById = new ConcurrentHashMap<Integer, Pair<Task, DispatcherQueue>>();
	private Map<Pair<ExecService, Facility>, Task> tasksByServiceAndFacility = new ConcurrentHashMap<Pair<ExecService, Facility>, Task>();
    private Map<TaskStatus, List<Task>> pool = new EnumMap<TaskStatus, List<Task>>(TaskStatus.class);
	
    public SchedulingPoolImpl() {
    	for(TaskStatus status : TaskStatus.class.getEnumConstants()) {
    		pool.put(status, new ArrayList<Task>());
    	}
    }

    @Override
	public int getSize() {
		return pool.size();
	}

	@Override
	public int addToPool(Task task, DispatcherQueue dispatcherQueue) {
		// XXX needs to be synchronized, but on what?
		synchronized(tasksById) {
			if(!tasksById.containsKey(task.getId())) {
				tasksById.put(task.getId(), new Pair<Task, DispatcherQueue>(task, dispatcherQueue));
				tasksByServiceAndFacility.put(new Pair<ExecService, Facility>(task.getExecService(), task.getFacility()), task);
				pool.get(task.getStatus()).add(task);
			}
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
		// TODO Auto-generated method stub

	}

	@Override
	public List<Task> getWaitingTasks() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		
	}

}
