package cz.metacentrum.perun.engine.unit;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.util.Assert;

import cz.metacentrum.perun.core.api.Destination;
import cz.metacentrum.perun.engine.TestBase;
import cz.metacentrum.perun.engine.scheduling.ExecutorEngineWorker;
import cz.metacentrum.perun.engine.scheduling.TaskResultListener;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.TaskResult;

public class ExecutorEngineWorkerImplTest extends TestBase implements TaskResultListener {

	@Autowired
	private Destination destination1;
	@Autowired
    private BeanFactory beanFactory;
	@Autowired
	private Task task1;
	private int count = 0;
	
    @IfProfileValue(name="perun.test.groups", values=("unit-tests"))
	@Test
	public void runTest() {
		ExecutorEngineWorker worker = (ExecutorEngineWorker) beanFactory.getBean("executorEngineWorker");
		worker.setTask(task1);
		worker.setExecService(task1.getExecService());
		worker.setFacility(task1.getFacility());
		worker.setDestination(destination1);
		worker.setResultListener(this);
		worker.run();
		Assert.isTrue(count == 1, "count 1");
	}

	@Override
	public void onTaskDestinationDone(Task task, Destination destination,
			TaskResult result) {
		count += 1;
	}

	@Override
	public void onTaskDestinationError(Task task, Destination destination,
			TaskResult result) {
		Assert.isTrue(false);;
	}
}
