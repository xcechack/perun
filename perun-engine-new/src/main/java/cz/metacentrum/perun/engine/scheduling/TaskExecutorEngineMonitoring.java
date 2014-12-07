package cz.metacentrum.perun.engine.scheduling;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
/**
*
* @author Jana Cechackova
*/
public interface TaskExecutorEngineMonitoring {
    
    public CopyOnWriteArraySet<ExecutorEngineWorker> getRunningWorkers();
    public CopyOnWriteArraySet<ExecutorEngineWorker> getFinishedWorkers();
    public BlockingQueue<Runnable> getWaitingWorkers();
    
    
}