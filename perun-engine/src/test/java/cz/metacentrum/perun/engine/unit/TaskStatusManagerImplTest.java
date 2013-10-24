package cz.metacentrum.perun.engine.unit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.metacentrum.perun.engine.TestBase;
import cz.metacentrum.perun.engine.scheduling.TaskStatus;
import cz.metacentrum.perun.engine.scheduling.TaskStatusManager;
import cz.metacentrum.perun.taskslib.model.Task;

public class TaskStatusManagerImplTest extends TestBase {

	@Autowired 
	TaskStatusManager taskStatusManager;
	@Autowired
	Task task1;
	
	@Test
	public void getTaskStatusTest() {
		TaskStatus taskStatus1 = taskStatusManager.getTaskStatus(task1);
		TaskStatus taskStatus2 = taskStatusManager.getTaskStatus(task1);
		Assert.isTrue(taskStatus1 == taskStatus2);
	}
	
	@Test
	public void clearTaskStatusTest() {
		TaskStatus taskStatus1 = taskStatusManager.getTaskStatus(task1);
		taskStatusManager.clearTaskStatus(task1);
		TaskStatus taskStatus2 = taskStatusManager.getTaskStatus(task1);
		Assert.isTrue(taskStatus1 != taskStatus2);
	}
	
}
