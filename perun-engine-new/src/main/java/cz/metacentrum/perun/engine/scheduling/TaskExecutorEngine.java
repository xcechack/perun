package cz.metacentrum.perun.engine.scheduling;

import org.springframework.core.task.TaskExecutor;

/**
 * 
 * @author Michal Karm Babacek JavaDoc coming soon...
 * 
 */
public interface TaskExecutorEngine {

	void beginExecuting();

	public DependenciesResolver getDependencyResolver();

	public void setDependencyResolver(DependenciesResolver dependencyResolver);

	public TaskStatusManager getTaskStatusManager();

	public void setTaskStatusManager(TaskStatusManager taskStatusManager);

	public SchedulingPool getSchedulingPool();

	public void setSchedulingPool(SchedulingPool schedulingPool);

	public void setTaskExecutorSendWorkers(TaskExecutor sendWorkers);

	public void setTaskExecutorGenWorkers(TaskExecutor genWorkers);
}
