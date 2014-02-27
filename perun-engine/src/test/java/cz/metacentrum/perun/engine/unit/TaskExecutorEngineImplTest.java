package cz.metacentrum.perun.engine.unit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.IfProfileValue;

import cz.metacentrum.perun.core.api.Destination;
import cz.metacentrum.perun.engine.TestBase;
import cz.metacentrum.perun.engine.scheduling.SchedulingPool;
import cz.metacentrum.perun.engine.scheduling.TaskExecutorEngine;
import cz.metacentrum.perun.engine.scheduling.TaskResultListener;
import cz.metacentrum.perun.engine.scheduling.TaskStatusManager;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;
import cz.metacentrum.perun.taskslib.model.TaskResult;

public class TaskExecutorEngineImplTest extends TestBase {

	@Autowired
	private Task task1;
    @Autowired
    private SchedulingPool schedulingPool;
    @Autowired
    private TaskExecutorEngine taskExecutorEngine;
    @Autowired
    private TaskStatusManager taskStatusManager; 
    
    @Before
    public void setup() {
    	task1.setStatus(TaskStatus.NONE);
    	schedulingPool.addToPool(task1);
    	schedulingPool.setTaskStatus(task1, TaskStatus.PLANNED);
    }
    
    @After
    public void cleanup() {
    	schedulingPool.removeTask(task1);
    }

    @IfProfileValue(name="perun.test.groups", values=("unit-tests"))
	@Test
	public void beginExecutingTest() {
    	taskExecutorEngine.beginExecuting();
    }
    
}
