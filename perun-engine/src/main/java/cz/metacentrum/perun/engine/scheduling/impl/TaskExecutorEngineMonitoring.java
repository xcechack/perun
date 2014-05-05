/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.metacentrum.perun.engine.scheduling.impl;

import cz.metacentrum.perun.engine.scheduling.ExecutorEngineWorker;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.Task;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Janicka
 */
public class TaskExecutorEngineMonitoring {
    
    private final static Logger log = LoggerFactory.getLogger(TaskStatusManagerImpl.class);
    	
    private HashSet<ExecutorEngineWorker> runningGenWorkers; //should work with ExecServices or Workers??
    private HashSet<ExecutorEngineWorker> runningSendWorkers; //exchange Lists for Sets??
    private BlockingQueue<ExecutorEngineWorker> waitingGenWorkers;
    private BlockingQueue<ExecutorEngineWorker> waitingSendWorkers;
    
    private List<ExecutorEngineWorker> doneGenWorkers;
    private List<ExecutorEngineWorker> doneSendWorkers;
        
    //Constants are taken from application-context.xml
    
    private static final int maxGenQueue = 2000; //or TaskExecutorGenWorkers.getMaxPoolSize() ??
    private static final int maxSendQueue = 200000; //or TaskExecutorSendWorkers.getMaxPoolSize() ??
    private static final int maxRunningGen = 5;
    private static final int maxRunningSend = 5;
    
    private boolean blockMe; //to assure that if some worker is done, next worker will be added from queue, NOT next new coming one
                             // block me separated for GEN and SEND?????
    
    
    /**CONSTRUCTOR
     * 
     * - initializes all collections
     */
    
    public TaskExecutorEngineMonitoring() {
        
        runningGenWorkers = new HashSet<>(maxRunningGen);
        runningSendWorkers = new HashSet<>(maxRunningSend);//should it be also blockingQueue because of limited capacity??
        
        waitingSendWorkers = new ArrayBlockingQueue<>(maxGenQueue);
        waitingGenWorkers = new ArrayBlockingQueue<>(maxSendQueue);
        
        doneGenWorkers = new ArrayList<>();
        doneSendWorkers = new ArrayList<>();
                
        blockMe = false;
        
		
    }
    
      
    
   /* public void addWorker(ExecutorEngineWorker worker)
    * 
    * - gets worker and decides, what type is it
    * - METHOD IS FINISHED
    */
    public void addWorker(ExecutorEngineWorker worker){
        
         
        if (worker.getExecService().getExecServiceType().equals(ExecService.ExecServiceType.GENERATE)){
            // Worker's type is GENERATE, trying to execute..
            addGenWorker(worker);
        }
        else if (worker.getExecService().getExecServiceType().equals(ExecService.ExecServiceType.SEND)){
            // Worker's type is SEND, trying to execute..
            addSendWorker(worker);
        }
        
    
    }
        
    
    /* public void addGenWorker (ExecutorEngineWorker worker)
     * 
     * - gets GENERATE type worker and decides, where it belongs (to running, waiting or rejected workers)
     * - then puts it into right collection
     * - METHOD IS FINISHED        
     */
    public void addGenWorker(ExecutorEngineWorker worker){
        
        if (!(waitingGenWorkers.size()<maxGenQueue)) {
            //Everything is full, worker is rejected.            
        }
        
        else if (blockMe || !waitingGenWorkers.isEmpty()) {
            //Worker is added to queue and waits.
            waitingGenWorkers.add(worker);
        } 
        
        else{
            //Worker is executed and runs.
            runningGenWorkers.add(worker);
        }
        
    }
    
    
     /* public void addSendWorker (ExecutorEngineWorker worker)
     * 
     * - gets SEND type worker and decides, where it belongs (to running, waiting or rejected workers)
     * - then puts it into right collection
     * - METHOD IS FINISHED        
     */
    
    synchronized public void addSendWorker(ExecutorEngineWorker worker){
             
        if (!blockMe && runningSendWorkers.size()<maxRunningSend){
            //Worker is executed and runs.
            runningSendWorkers.add(worker);
        }
        else if (waitingSendWorkers.size()<maxSendQueue) {
            //Worker is added to queue and waits.
            waitingSendWorkers.add(worker);
        }
        else{
            //everything is full and worker is rejected
        }
        
    }
    
   
    
    /* public void reportFinishedWorker(ExecutorEngineWorker worker)
    * 
    * - gets worker and decides, what type is it
    * - METHOD IS FINISHED
    */       
    public void reportFinishedWorker(ExecutorEngineWorker worker){
        
         if (worker.getExecService().getExecServiceType().equals(ExecService.ExecServiceType.GENERATE)){
                                  
            deleteGenWorker(worker);
        }
        
        else if (worker.getExecService().getExecServiceType().equals(ExecService.ExecServiceType.SEND)){
                        
            deleteSendWorker(worker);
        }
    
    }
    
    
    /* public void deleteGenWorker (ExecutorEngineWorker worker)
     * 
     * - deletes GENERATE worker from collection of running workers, because worker has ended
     * - blockMe:   is true, when there is some worker waiting in the queue to assure, that worker in the queue is executed, not some new one coming in the same time
     * - after deleting, worker is added to collection of done workers, so we don't forget information about it
     * - throws Exception and I don't know why. WILL BE FIGURATED OUT SOON
     */
    public void deleteGenWorker(ExecutorEngineWorker worker){
        // deletes worker from List (Set) and if something is waiting in a queue, add it instead
        
        if (!waitingGenWorkers.isEmpty()){ 
        blockMe = true;
        
        runningGenWorkers.remove(worker);
        
        try {
            // takes the top worker from the queue and executes it
            runningGenWorkers.add(waitingGenWorkers.take());
        } catch (InterruptedException ex){
            log.error(ex.toString());
        }
            blockMe = false;
            
        }
        else runningGenWorkers.remove(worker);
        
        doneGenWorkers.add(worker);
    }
    
    
    /* public void deleteSendWorker (ExecutorEngineWorker worker)
     * 
     * - deletes SEND worker from collection of running workers, because worker has ended
     * - blockMe:   is true, when there is some worker waiting in the queue to assure, that worker in the queue is executed, not some new one coming in the same time
     * - after deleting, worker is added to collection of done workers, so we don't forget information about it
     * - throws Exception and I don't know why. WILL BE FIGURATED OUT SOON
     */
    synchronized public void deleteSendWorker(ExecutorEngineWorker worker){
        // deletes worker from List (Set) and if something is waiting in a queue, add it instead        
        
        if (!waitingSendWorkers.isEmpty()){ 
            blockMe = true;
        
            runningSendWorkers.remove(worker);
        
            try{
            // takes the top worker from the queue and executes it
               runningSendWorkers.add(waitingSendWorkers.take());
             } catch (InterruptedException ex){
                    log.error(ex.toString());
            }
            blockMe = false;
        }
        else runningSendWorkers.remove(worker);
        
        doneSendWorkers.add(worker);
    }
     
    
    /** public void writeInfo() 
     * - method for complex info about all actual and past workers including statistics 
     * 
     */
    public void writeInfo() throws IllegalStateException{
        
        //some complex message about all workers
        int genDone = 0;
        int genError = 0;
        int genRunning = runningGenWorkers.size();
        int genWaiting = waitingGenWorkers.size();
        
        int sendDone = 0;
        int sendError = 0;
        int sendRunning = runningSendWorkers.size();
        int sendWaiting = waitingSendWorkers.size();
        
        Task task;
                       
        for (int i=0; i<doneGenWorkers.size();i++){
            
            task = doneGenWorkers.get(i).getTask();
                                          
            if(task.getStatus()==Task.TaskStatus.DONE)    genDone++;
            else if(task.getStatus()==Task.TaskStatus.ERROR)     genError++;
                
            else    throw new IllegalStateException("expected DONE or ERROR status on task: " + task.toString());
                      
            }
        
            
         for (int i=0; i<doneSendWorkers.size();i++){    
             
             task = doneSendWorkers.get(i).getTask();
                                
             if(task.getStatus()==Task.TaskStatus.DONE)    sendDone++;
                
                else if(task.getStatus()==Task.TaskStatus.ERROR)     sendError++;
                
                else    throw new IllegalStateException("expected DONE or ERROR status"+ task.toString());
                       
            }
        
                 
        int sumGen = genRunning+genWaiting+genDone+genError;
        int sumSend = sendRunning+sendWaiting+sendDone+sendError;
        int sum = sumGen + sumSend;
        
        System.out.println("\n\n\n-------------------WORKERS INFO-------------------\n"
                             
              +"total numbers of workers is " + sum);
              
        System.out.println("\n-------------------GEN WORKERS-------------------\n"
              
             +"gen workers running in the pool: "+ runningGenWorkers.size()
              
              +"\nother workers waiting in the queue: "+ waitingGenWorkers.size()
              
              +"\nnumber of successfully finished gen workers is: " + genDone
              +"\nnumber of failed gen workers is: " + genError);
              
        System.out.println("\n-------------------SEND WORKERS-------------------\n"
              
                +"gen workers running in the pool: "+ runningSendWorkers.size()
              
              +"\nother workers waiting in the queue: "+ waitingSendWorkers.size()
              
              +"\nnumber of successfully finished gen workers is: " + sendDone
              +"\nnumber of failed gen workers is: " + sendError);
              
             
        
                              
    }
    
    
    
    
    public HashSet<ExecutorEngineWorker> getRunningSendWorkers(){
        return runningSendWorkers;
    }
	
    public HashSet<ExecutorEngineWorker> getRunningGenWorkers(){
        return runningGenWorkers;
    }
    
    public BlockingQueue<ExecutorEngineWorker> getWaitingSendWorkers(){
        return waitingSendWorkers;
    }
    
    public BlockingQueue<ExecutorEngineWorker> getWaitingGenWorkers(){
        return waitingGenWorkers;
    }
    
    
	

    
}
