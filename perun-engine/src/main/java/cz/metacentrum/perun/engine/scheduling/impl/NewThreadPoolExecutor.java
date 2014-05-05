/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.metacentrum.perun.engine.scheduling.impl;

import cz.metacentrum.perun.engine.scheduling.ExecutorEngineWorker;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Janicka
 */
public class NewThreadPoolExecutor extends ThreadPoolExecutor{
    
    /**
     * @param activeWorkers     all workers running or waiting in queue
     * @param runningWorkers    workers with state RUNNING
     * @param completedWorkes   all finished workers
     * @param runLock           locks method run() in class ExecutorEngineWorkerImpl to not change status during getting information
     * @param changeLock        locks all changes between collections - in future there can be 3 locks, each for different change
     * 
     * both runLock and changeLock are ReadWriteLocks and deny access only if method readingLock().lock() is called (method names are switched)
     */
    private final CopyOnWriteArraySet<Runnable> activeWorkers; 
    private final CopyOnWriteArraySet<ExecutorEngineWorkerImpl> runningWorkers;
    private final CopyOnWriteArraySet<Runnable> completedWorkers;
    private final WorkerLock runLock= new WorkerLock();
    private final WorkerLock changeLock = new WorkerLock();
    
    
    //constructor
    public NewThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        activeWorkers = new CopyOnWriteArraySet <>();
        runningWorkers = new CopyOnWriteArraySet <>();
        completedWorkers = new CopyOnWriteArraySet <>();
        
    }
    //constructor
    public NewThreadPoolExecutor(int corePoolSize, int maxPoolSize, int keepAliveSeconds, TimeUnit timeUnit, BlockingQueue<Runnable> queue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
       super(corePoolSize, maxPoolSize, keepAliveSeconds, timeUnit, queue, threadFactory, rejectedExecutionHandler);
       activeWorkers = new CopyOnWriteArraySet <>();
       runningWorkers = new CopyOnWriteArraySet <>();
       completedWorkers = new CopyOnWriteArraySet <>();
    }
    
   
   // this method is not used
   public HashSet<Runnable> getWorkers(){
        Field field = null;
        
////        TRIED TO ACCESS PRIVATE INNER CLASS "WORKER" IN THREADPOOLEXECUTOR
//        
////        Class<?>[] FieldClass = null;
////        String className = this.getClass().getSuperclass().getName()+"$Worker";
////        HashSet<java.util.concurrent.ThreadPoolExecutor$Worker> workers;
////
////        
////        FieldClass = this.getClass().getSuperclass().getDeclaredClasses();
////        
////        for (int i = 0; i<FieldClass.length;i++){
////            
////            if (FieldClass[i].getName().equals(className)) {
////                System.out.println(className);
////                }
////         }
//        
//     try {            
//            
//         // TRIED TO ACCESS HASHSET <WORKERS> AND JUST PRINT IT
//            field = this.getClass().getSuperclass().getDeclaredField("workers");
//            field.setAccessible(true);
//            
//            //Object obj = field.get(this.getClass().getSuperclass());
//            //       System.out.println(obj.toString());
//                    
//            
//                    
//            
////            System.out.println(field.isAccessible());
//            
            
//        } catch (NoSuchFieldException ex) {
//            Logger.getLogger(NewThreadPoolExecutor.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (SecurityException ex) {
//            Logger.getLogger(NewThreadPoolExecutor.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(NewThreadPoolExecutor.class.getName()).log(Level.SEVERE, null, ex);
//        } finally{
//            
//            if (field!=null) field.setAccessible(false);
        
            
            
//        for (int i=0; i<fields.length; i++){
//            if(fields[i].getName().equals("workers")) {
//                
//                workers = fields[i];
//                workers.setAccessible(true);
//                try {
//                    newWorkers = (HashSet<Runnable>)workers.get(this.getClass().getSuperclass());
//                    System.out.println("workers maybe saved");
//                } catch (IllegalArgumentException ex) {
//                    Logger.getLogger(NewThreadPoolExecutor.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (IllegalAccessException ex) {
//                    Logger.getLogger(NewThreadPoolExecutor.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                
//            };
//        }
      return null;
    }
    
 
   //this method is always called before execution
   @Override 
   public void beforeExecute(Thread t, Runnable r){
        
        try{
            changeLock.writingLock().lock(); // because we want to avoid mistake, that worker is not in any collection
            synchronized(activeWorkers){
                activeWorkers.remove(r);
             }
            synchronized (runningWorkers){
                runningWorkers.add((ExecutorEngineWorkerImpl)r);
            }
        }finally{
            changeLock.writingLock().unlock();
        }
       ExecutorEngineWorkerImpl worker = (ExecutorEngineWorkerImpl) r; 
       worker.setLock(runLock); //locks method run() in ExecutorEngineWorker to not change status during getting information
        
        super.beforeExecute(t, r);
   }
    
   //this method is always called when new task comes 
    @Override
   public void execute(Runnable r){
       try{
           changeLock.writingLock().lock(); 
       synchronized(activeWorkers){
           activeWorkers.add(r);
       }
       super.execute(r);
       }finally{
           changeLock.writingLock().unlock();
       }
   }
   
   //this method is always called after execution
    @Override
    public void afterExecute (Runnable r, Throwable t){
        
        try{
            changeLock.writingLock().lock(); //princip is the same as with beforeExecute() method
            synchronized (runningWorkers){
                runningWorkers.remove(r);
            }
            synchronized(completedWorkers){
                completedWorkers.add(r);
            }
        }finally{
            changeLock.writingLock().unlock();
        }
        super.afterExecute(r, t);
        
        
    }
    

    
    @Override
    public String toString(){
        Date date = new Date();
        System.out.println("THREAD POOL EXECUTOR INFO\nactual time: "+date.toString()+"\n\nACTIVE WORKERS WAITING FOR EXECUTION:");
        synchronized (super.getQueue()){
        try{
            changeLock.readingLock().lock();
            runLock.readingLock().lock(); //these 2 locks are locking all operations and changes in this class, and also in ExecutorEngineWorkerImpl class (method run())
                synchronized (activeWorkers){
                    
                        for (Runnable worker : activeWorkers){
                             if(!super.getQueue().contains(worker))  System.out.println("WORKER INFO: "+worker.toString());
                        }
                    
                }
        
            System.out.println("\nRUNNING WORKERS: ");
            synchronized (runningWorkers){
                for (ExecutorEngineWorkerImpl worker : runningWorkers){
                    System.out.println("WORKER INFO: "+ worker.runningToString()) ;//different toString() because: we can have task in runningWorkers earlier than it has status RUNNING
                }
            }
        
        
        System.out.println("\nWORKERS IN QUEUE:");
        
            for (Object worker : super.getQueue()){
                if(!runningWorkers.contains(worker)) System.out.println("WORKER INFO: "+worker.toString());            
            }
        
        
        System.out.println("ACTIVE COUNT: "+super.getActiveCount()+" COMPLETED TASK COUNT: "+super.getCompletedTaskCount()+" TASK COUNT: "+super.getTaskCount());
        System.out.println("---------------------------------------------\n");
        
        }finally{
            changeLock.readingLock().unlock();
            runLock.readingLock().unlock();
        }
        }
    
        return null;
    }
    
}
