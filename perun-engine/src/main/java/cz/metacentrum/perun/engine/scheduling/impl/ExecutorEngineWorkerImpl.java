package cz.metacentrum.perun.engine.scheduling.impl;


import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import cz.metacentrum.perun.engine.scheduling.impl.StreamGobbler;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;
import cz.metacentrum.perun.engine.scheduling.impl.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import cz.metacentrum.perun.core.api.Destination;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.engine.scheduling.ExecutorEngineWorker;
import cz.metacentrum.perun.engine.scheduling.SchedulingPool;
import cz.metacentrum.perun.engine.scheduling.TaskResultListener;
import org.springframework.transaction.annotation.Transactional;

import cz.metacentrum.perun.core.api.Destination;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.engine.scheduling.ExecutorEngineWorker;
import cz.metacentrum.perun.taskslib.dao.TaskResultDao;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.ExecService.ExecServiceType;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;
import cz.metacentrum.perun.taskslib.model.TaskResult;
import cz.metacentrum.perun.taskslib.model.TaskResult.TaskResultStatus;
import cz.metacentrum.perun.taskslib.service.TaskManager;


@Component("executorEngineWorker")
@Scope(value = "prototype")
public class ExecutorEngineWorkerImpl implements ExecutorEngineWorker {
    private final static Logger log = LoggerFactory.getLogger(ExecutorEngineWorkerImpl.class);


    private TaskResultListener resultListener;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private TaskResultDao taskResultDao;
    private Task task;
    private Facility facility;
    private ExecService execService;
    private Destination destination;
    @Autowired
    private Properties propertiesBean;
    private int engineId = -1;
    @Autowired
    private SchedulingPool schedulingPool;
    private WorkerStatus workerStatus;
   
    int id;

       
    
    @Override
    public void run()  {
               
        log.info("EXECUTING(worker:" + this.hashCode() + "): Task ID:" + task.getId() + ", Facility ID:" + task.getFacilityId() + ", ExecService ID:" + task.getExecServiceId() + ", ExecServiceType:"
                + execService.getExecServiceType());
        

        String stdout = null;
        String stderr = null;
        int returnCode = -1;
        if (execService.getExecServiceType().equals(ExecServiceType.GENERATE)) {
            ProcessBuilder pb = new ProcessBuilder(execService.getScript(), "-f", String.valueOf(task.getFacilityId()));
            pb.directory(new File("gen")); ///FIXME

            try {
                Process process = pb.start();

                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
                StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());               
                
                errorGobbler.start();
                outputGobbler.start();
                
                returnCode = process.waitFor();
                
                while(errorGobbler.isAlive() || outputGobbler.isAlive()) Thread.sleep(50);
                
                stderr=errorGobbler.getSb();
                stdout=outputGobbler.getSb();

                // There is no point in writing into TaskResults. We switch TASK status at once.
                // TODO: Put some logic in here :-)

                task.setStatus(returnCode == 0 ? TaskStatus.DONE : TaskStatus.ERROR);
                task.setEndTime(new Date(System.currentTimeMillis()));
                if(task.getStatus().equals(TaskStatus.ERROR)) {
                  log.info("GEN task failed. Ret code " + returnCode + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);

                      	resultListener.onTaskDestinationError(task, destination, null);
                } else {
                	resultListener.onTaskDestinationDone(task, destination, null);

                
            }
            
            } catch (IOException e) {
                log.error(e.toString(), e);

                //task.setStatus(TaskStatus.ERROR);
                //task.setEndTime(new Date(System.currentTimeMillis()));
            } catch (Exception e) {
                log.error(e.toString(), e);
                //task.setStatus(TaskStatus.ERROR);
                //task.setEndTime(new Date(System.currentTimeMillis()));
            } finally {
                try {
                Thread.sleep(100); //pretends like it took 100ms to execute this worker
                } catch(InterruptedException ex) {
                  Thread.currentThread().interrupt();
                    }
                                 
                  task.setStatus(TaskStatus.DONE);
                  task.setEndTime(new Date(System.currentTimeMillis()));
                  String ret = returnCode == -1 ? "unknown" : String.valueOf(returnCode);
                  log.debug("GEN task ended. Ret code " + ret + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);
                  //taskManager.updateTask(task, getEngineId());
                  
            }             
        } else if (execService.getExecServiceType().equals(ExecServiceType.SEND)) {
            ProcessBuilder pb = new ProcessBuilder(execService.getScript(), facility.getName() + '-' + facility.getType(), destination.getDestination(), destination.getType());
            pb.directory(new File("send")); ///FIXME get from config file

            
            try {
                Process process = pb.start();
                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
                StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());               

                task.setStatus(TaskStatus.ERROR);
                task.setEndTime(new Date(System.currentTimeMillis()));
            } catch (Exception e) {
                log.error(e.toString(), e);
                task.setStatus(TaskStatus.ERROR);
                task.setEndTime(new Date(System.currentTimeMillis()));
            } finally {
                  String ret = returnCode == -1 ? "unknown" : String.valueOf(returnCode);
                  log.debug("GEN task ended. Ret code " + ret + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);
                taskManager.updateTask(task, getEngineId());
            }
        } else if (execService.getExecServiceType().equals(ExecServiceType.SEND)) {

            ProcessBuilder pb = new ProcessBuilder(execService.getScript(), facility.getName() + '-' + facility.getType(), destination.getDestination(), destination.getType());
            pb.directory(new File("send")); ///FIXME get from config file

            try {
                Process process = pb.start();

                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream());
                StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());               

                errorGobbler.start();
                outputGobbler.start();
                
                returnCode = process.waitFor();
                
                while(errorGobbler.isAlive() || outputGobbler.isAlive()) Thread.sleep(50);

                stderr=errorGobbler.getSb();
                stdout=outputGobbler.getSb();

                TaskResult taskResult = new TaskResult();
                taskResult.setTaskId(task.getId());
                taskResult.setDestinationId(destination.getId());
                taskResult.setErrorMessage(stderr);
                 //According to RT 30609, STDOUT should be only logged now.

                log.debug(stdout.toString());
                taskResult.setStandardMessage("See debug log.");
                taskResult.setReturnCode(returnCode);
                taskResult.setStatus(returnCode == 0 ? TaskResultStatus.DONE : TaskResultStatus.ERROR);
                taskResult.setTimestamp(new Date(System.currentTimeMillis()));


                task.setEndTime(new Date(System.currentTimeMillis()));
                
                log.debug("inserting taskResult " + taskResult.toString());

                taskResultDao.insertNewTaskResult(taskResult, getEngineId());

                if(taskResult.getStatus().equals(TaskStatus.ERROR)) {
                  log.info("SEND task failed. Ret code " + returnCode + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);

                  resultListener.onTaskDestinationError(task, destination, taskResult);
                } else {
                    log.info("SEND task completed. Ret code " + returnCode + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);
                	resultListener.onTaskDestinationDone(task, destination, taskResult);
                }

            } catch (Exception e) {
                log.error(e.toString());    
            }
                         
                
//                log.info("SEND task ended. Ret code " + returnCode + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);
//                log.error("ERROR with TASK ID: " + task.getId() + " , Exception:" + e.toString(), e);
//                // If we are unable to switch to the ERROR state, PropagationMaintainer would resolve
//                // the Tasks status correctly anyhow (count Destinations x count TaskResults)
//                
//                TaskResult taskResult = new TaskResult();
//                taskResult.setTaskId(task.getId());
//                taskResult.setDestinationId(destination.getId());
//                taskResult.setStatus(TaskResultStatus.ERROR);
//                taskResult.setTimestamp(new Date(System.currentTimeMillis()));
//                resultListener.onTaskDestinationError(task, destination, taskResult);
//                try {
//                	taskResultDao.insertNewTaskResult(taskResult, getEngineId());
//                } catch (InternalErrorException e1) {
//                    log.error(e.toString(), e);
//                }
             finally {
                    try {
                Thread.sleep(100); //pretends like it took 100ms to execute this worker
                } catch(InterruptedException ex) {
                  Thread.currentThread().interrupt();
		}
                   
                    TaskResult taskResult = new TaskResult();
                    taskResult.setTaskId(task.getId());
                    taskResult.setDestinationId(destination.getId());
                    taskResult.setErrorMessage(stderr);
                    //According to RT 30609, STDOUT should be only logged now.
                
                    //log.debug(stdout.toString());
                    taskResult.setStandardMessage("See debug log.");
                    taskResult.setReturnCode(returnCode);
                    
                    taskResult.setStatus(TaskResultStatus.DONE);
                                        
                    taskResult.setTimestamp(new Date(System.currentTimeMillis()));

                  task.setEndTime(new Date(System.currentTimeMillis()));
                  String ret = returnCode == -1 ? "unknown" : String.valueOf(returnCode);
                  
                  log.info("SEND task completed. Ret code " + returnCode + ". STDOUT: {}  STDERR: {}. Task: " + task, stdout, stderr);
                  resultListener.onTaskDestinationDone(task, destination, taskResult);

                }

            


        } else {
            throw new IllegalArgumentException("Expected ExecService type is SEND or GENERATE.");
        }
    }


    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }


    public TaskResultDao getTaskResultDao() {
        return taskResultDao;
    }

    public void setTaskResultDao(TaskResultDao taskResultDao) {
        this.taskResultDao = taskResultDao;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public ExecService getExecService() {
        return execService;
    }

    public void setExecService(ExecService execService) {
        this.execService = execService;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Properties getPropertiesBean() {
        return propertiesBean;
    }

    public void setPropertiesBean(Properties propertiesBean) {
        this.propertiesBean = propertiesBean;
    }

    public int getEngineId() {

    	if (engineId == -1) {
        this.engineId = Integer.parseInt(propertiesBean.getProperty("engine.unique.id"));
        }
        return engineId;
    }


	public TaskResultListener getResultListener() {
		return resultListener;
	}

	public void setResultListener(TaskResultListener resultListener) {
		this.resultListener = resultListener;
	}
        
       
        public String toString(){
        
            return ("Worker ID: "+this.id+ " STATUS: "+this.workerStatus+"      Worker details: Task ID: "+this.getTask().getId()+" TASK TYPE: "+this.getExecService().getExecServiceType()+ " FACILITY: "+this.getFacility() +" DESTINATION: "+this.getDestination());
            
        }
        
       public void setID(int id){
        
            this.id = id;
        }
        
        public void setWorkerStatus(WorkerStatus status){
            this.workerStatus = status;
        }
        
        public WorkerStatus getWorkerStatus (){
            return this.workerStatus;
        }

   
}
