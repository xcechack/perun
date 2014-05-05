package cz.metacentrum.perun.engine.scheduling;

import cz.metacentrum.perun.core.api.Destination;
import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.engine.scheduling.impl.TaskExecutorEngineMonitoring;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.Task;

/**
 * 
 * @author Michal Karm Babacek JavaDoc coming soon...
 * 
 */
public interface ExecutorEngineWorker extends Runnable {
    void setTask(Task task);
    
    Task getTask();

    void setDestination(Destination destination);

    void setExecService(ExecService execService);
    
    ExecService getExecService();
    
    void setFacility(Facility facility);

    void setResultListener(TaskResultListener resultListener);
    
    TaskResultListener getResultListener();
    
    void setID(int id);
    
    @Override
    String toString();
    
    String runningToString();
    
    String completedToString();
}
