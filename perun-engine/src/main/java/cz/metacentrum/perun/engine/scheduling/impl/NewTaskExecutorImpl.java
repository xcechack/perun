/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.metacentrum.perun.engine.scheduling.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 *
 * @author Janicka
 */
public class NewTaskExecutorImpl extends ThreadPoolTaskExecutor {
    
    public static final int queueCapacity = 2000;
    
    private NewThreadPoolExecutor executor;
    

   
//    @Override
//    public void execute(Runnable r) {
//        
//        super.execute(r);
////        this.getThreadPoolExecutor().toString();
//        //AFTER EACH CHANGE IN THREADPOOLEXECUTOR - THERE IS CONTROL OUTPUT      
////        executor.toString();
//        
//    }
    
     @Override
     protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler){
            
         BlockingQueue<Runnable> queue = super.createQueue(queueCapacity);
         
         executor = new NewThreadPoolExecutor(super.getCorePoolSize(), super.getMaxPoolSize(), 
                                            super.getKeepAliveSeconds(), TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);
         
            return executor;
        }
     
     
     @Override
     public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException{
         
         Assert.state(executor != null, "ThreadPoolTaskExecutor not initialized.");
         return executor;
     }
    

     public void printAndWait(int time){
      try{
        
        getThreadPoolExecutor().toString();
        
        Thread.sleep(time);
        
        } catch (InterruptedException ex){
            System.err.println(ex.toString());
        }
        
    }
    
}
