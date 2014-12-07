package cz.metacentrum.perun.engine.scheduling.impl;

import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
*
* @author Jana Cechackova
*/
public class WorkerLock extends ReentrantReadWriteLock {
    public WriteLock readingLock(){
        return super.writeLock();
    }
    
    public ReadLock writingLock(){
        return super.readLock();
    }
}