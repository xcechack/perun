package cz.metacentrum.perun.engine.scheduling.impl;

import cz.metacentrum.perun.engine.scheduling.ExecutorEngineWorker;
import cz.metacentrum.perun.engine.scheduling.TaskExecutorEngineMonitoring;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
/**
*
* @author Jana Cechackova
*/
public class TaskExecutorEngineMonitoringImpl extends ThreadPoolExecutor implements TaskExecutorEngineMonitoring{
/**
* @param runningWorkers     workers with state RUNNING
* @param completedWorkes    workers with state FINISHED
* @param changeLock         locks all changes between collections - in future there can be 3 locks, each for different change
*/
    @Autowired
    private CopyOnWriteArraySet<ExecutorEngineWorker> runningWorkers;
    @Autowired
    private CopyOnWriteArraySet<ExecutorEngineWorker> completedWorkers;
    @Autowired
    private WorkerLock changeLock;

    public TaskExecutorEngineMonitoringImpl(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }
    
    public TaskExecutorEngineMonitoringImpl(int corePoolSize, int maxPoolSize, int keepAliveSeconds, TimeUnit timeUnit, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(corePoolSize, maxPoolSize, keepAliveSeconds, timeUnit, queue, threadFactory, rejectedExecutionHandler);
    }
    
    @Override
    public void execute(Runnable r){
        try{
            changeLock.writingLock().lock();
            ExecutorEngineWorker worker = (ExecutorEngineWorker) r;
            worker.setStatus(WorkerStatus.WAITING);
        }finally{
            changeLock.writingLock().unlock();
        }
        super.execute(r);
}
    
    @Override
    public void beforeExecute(Thread t, Runnable r){
        try{
            changeLock.writingLock().lock();
            ExecutorEngineWorker worker = (ExecutorEngineWorker)r;
            worker.setStatus(WorkerStatus.RUNNING);
            runningWorkers.add(worker);
        }finally{
            changeLock.writingLock().unlock();
        }
        super.beforeExecute(t, r);
}
    
    @Override
    public void afterExecute (Runnable r, Throwable t){
        try{
            changeLock.writingLock().lock();
            ExecutorEngineWorker worker = (ExecutorEngineWorker) r;
            runningWorkers.remove((ExecutorEngineWorkerImpl)r);
            worker.setStatus(WorkerStatus.FINISHED);
            completedWorkers.add(worker);
        }finally{
            changeLock.writingLock().unlock();
        }
        super.afterExecute(r, t);
    }
    
    @Override
    public CopyOnWriteArraySet<ExecutorEngineWorker> getFinishedWorkers(){
        return completedWorkers;
    }
    
    @Override
    public BlockingQueue<Runnable> getWaitingWorkers(){
        return super.getQueue();
    }
    
    @Override
    public CopyOnWriteArraySet<ExecutorEngineWorker> getRunningWorkers(){
        try{
            changeLock.readingLock().lock();
            return runningWorkers;
        }
        finally{
            changeLock.readingLock().unlock();
        }
            
    }
}