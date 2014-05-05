/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.metacentrum.perun.engine.scheduling.impl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Janicka
 */
public class WorkerLock extends ReentrantReadWriteLock {
    
    
    
    
    public WriteLock readingLock(){
        
        return super.writeLock();
        
    }
    
    public ReadLock writingLock(){
    
        return super.readLock();
        
    }
    
}
