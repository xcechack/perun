/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.metacentrum.perun.engine.scheduling;

import org.springframework.core.task.TaskExecutor;

/**
 *
 * @author Janicka
 */
public interface NewTaskExecutor extends TaskExecutor {
    
    @Override
    public void execute(Runnable r);
    
}
