package cz.metacentrum.perun.engine.unit;

import org.springframework.beans.factory.annotation.Autowired;
import cz.metacentrum.perun.engine.TestBase;
import cz.metacentrum.perun.engine.scheduling.SchedulingPool;
import cz.metacentrum.perun.engine.scheduling.TaskExecutorEngine;
import cz.metacentrum.perun.engine.scheduling.impl.MonitoringTaskExecutorImpl;
import cz.metacentrum.perun.taskslib.dao.TaskResultDao;
import cz.metacentrum.perun.taskslib.model.Task;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for monitoring Engine
 * @author Jana Cechackova
 */
public class TaskExecutorEngineMonitoringTest extends TestBase {
    
    @Autowired
    private Task task1;
    @Autowired
    private Task task2;
    @Autowired
    private Task task4;
    @Autowired
    private Task task5;
    @Autowired
    private Task task6;
    
    @Autowired
    private Task task_gen;
    
    @Autowired
    private TaskExecutorEngine taskExecutorEngine;
    
    @Autowired
    private SchedulingPool schedulingPool;
    
    @Autowired
    TaskResultDao taskResultDao;
    
   
    
    @Before
    public void setup(){
        //TRY TO ADD MONITORING
      
        
        task1.setStatus(Task.TaskStatus.NONE);
        schedulingPool.addToPool(task1);
        schedulingPool.setTaskStatus(task1, Task.TaskStatus.PLANNED);
        
        task2.setStatus(Task.TaskStatus.NONE);
        schedulingPool.addToPool(task2);
        schedulingPool.setTaskStatus(task2, Task.TaskStatus.PLANNED);
        
        task_gen.setStatus(Task.TaskStatus.NONE);
        schedulingPool.addToPool(task_gen);
        schedulingPool.setTaskStatus(task_gen, Task.TaskStatus.PLANNED);
        
        task4.setStatus(Task.TaskStatus.NONE);
        schedulingPool.addToPool(task4);
        schedulingPool.setTaskStatus(task4, Task.TaskStatus.PLANNED);
        
        task5.setStatus(Task.TaskStatus.NONE);
        schedulingPool.addToPool(task5);
        schedulingPool.setTaskStatus(task5, Task.TaskStatus.PLANNED);
        
        task6.setStatus(Task.TaskStatus.NONE);
        schedulingPool.addToPool(task6);
        schedulingPool.setTaskStatus(task6, Task.TaskStatus.PLANNED);
        
      
              
    }
    
    @After
    public void cleanup() {
    	taskResultDao.clearAll();
    	schedulingPool.removeTask(task1);
        schedulingPool.removeTask(task2);
        schedulingPool.removeTask(task_gen);
        schedulingPool.removeTask(task4);
    }
    
    @Test
    public void simpleTest(){
        assertTrue("there are 6 tasks", schedulingPool.getSize()==6);
//        task1.setStatus(Task.TaskStatus.DONE);
//        System.out.println("planned tasks:"+schedulingPool.getPlannedTasks().size());
//        System.out.println("done tasks:"+ schedulingPool.getDoneTasks().size());
//        assertTrue( "only one planned task", schedulingPool.getDoneTasks().size()==1);
    
    }
    
    @Test
    public void beginExecutingTest(){
//        System.out.println("size of runningsend "+taskExecutorEngineMonitoring.getRunningSendWorkers().size());
        taskExecutorEngine.beginExecuting();
       
        MonitoringTaskExecutorImpl executor = (MonitoringTaskExecutorImpl) taskExecutorEngine.getTaskExecutorSendWorkers();   
                
        executor.printAndWait(5, 1);
       
      
}
        
//        //assertTrue("planned tasks should be empty", schedulingPool.getPlannedTasks().isEmpty());
//        //Thread.sleep(200);
//        taskExecutorEngineMonitoring.writeInfo();
//        Thread.sleep(50);
//        taskExecutorEngineMonitoring.writeInfo();
//        Thread.sleep(50);
//        taskExecutorEngineMonitoring.writeInfo();
//        Thread.sleep(50);
//        taskExecutorEngineMonitoring.writeInfo();
//        Thread.sleep(50);
//        taskExecutorEngineMonitoring.writeInfo();
//        Thread.sleep(200);
////        Thread.sleep(500);
//        System.out.println("task1 "+task1.getStatus() );
//        System.out.println("task2 "+task2.getStatus() );
//        System.out.println("task3 "+task_gen.getStatus() );
        
        
        //assertTrue("all tasks should be done", schedulingPool.getDoneTasks().size()==3);

    /**
     *
     * @param time
     */
    
    
    
}
    
    
    

