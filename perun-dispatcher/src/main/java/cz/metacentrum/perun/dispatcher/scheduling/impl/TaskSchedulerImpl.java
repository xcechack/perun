package cz.metacentrum.perun.dispatcher.scheduling.impl;


import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cz.metacentrum.perun.core.api.Facility;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import cz.metacentrum.perun.core.api.exceptions.PrivilegeException;
import cz.metacentrum.perun.core.api.exceptions.ServiceNotExistsException;
import cz.metacentrum.perun.dispatcher.scheduling.DenialsResolver;
import cz.metacentrum.perun.dispatcher.scheduling.DependenciesResolver;
import cz.metacentrum.perun.dispatcher.scheduling.SchedulingPool;
import cz.metacentrum.perun.dispatcher.scheduling.TaskScheduler;
import cz.metacentrum.perun.taskslib.model.ExecService;
import cz.metacentrum.perun.taskslib.model.ExecService.ExecServiceType;
import cz.metacentrum.perun.taskslib.model.Task;
import cz.metacentrum.perun.taskslib.model.Task.TaskStatus;

@org.springframework.stereotype.Service(value = "taskScheduler")
public class TaskSchedulerImpl implements TaskScheduler {
	private final static Logger log = LoggerFactory.getLogger(TaskSchedulerImpl.class);

	@Autowired
	private SchedulingPool schedulingPool;
	@Autowired
	private DenialsResolver denialsResolver;
	@Autowired
	private DependenciesResolver dependenciesResolver;
	
	@Override
	public void processPool() throws InternalErrorException {
		for(Task task : schedulingPool.getWaitingTasks()) {
			scheduleTask(task);
		}
	}

	private void scheduleTask(Task task) {
		ExecService execService = task.getExecService();
		Facility facility = task.getFacility();
		Date time = new Date(System.currentTimeMillis());
		
		log.debug("Facility to be processed: " + facility.getId() + ", ExecService to be processed: " + execService.getId());

        Task previousTask = null;
        List<ExecService> dependantServices = null;
        List<ExecService> dependencies = null;
        
        // Is the ExecService enabled? (Global setting)
        // If it is not, we drop it and do nothing.
        // ############################################
        log.debug("Is the execService ID:" + execService.getId() + " enabled globally?");
        if (execService.isEnabled()) {
            log.debug("   Yes, it is globally enabled.");
            // Is the ExecService denied on this Facility?
            // If it is, we drop it and do nothing.
            // ###########################################
            log.debug("   Is the execService ID:" + execService.getId() + " denied on facility ID:" + facility.getId() + "?");
            if (!denialsResolver.isExecServiceDeniedOnFacility(execService, facility)) {
                log.debug("   No, it is not.");
                // Isn't there the same ExecService,Facility pair (Task) PROCESSING at the moment?
                // If it is, we drop it and do nothing (there is no point in scheduling it again at the moment).
                // #############################################################################################
                log.debug("   What is the previous task status for the execService ID:" + execService.getId() + ":facility ID:" + facility.getId() + " couple?");
                previousTask = schedulingPool.getTask(execService, facility);
                if (previousTask == null || !previousTask.getStatus().equals(TaskStatus.PROCESSING)) {
                    log.debug("   The previous status IS either null or IS NOT \"PROCESSING\".");
                    // If any of the ExecServices that depends on this one is running PROCESSING
                    // we will put the ExecService,Facility pair back to the pool.
                    // #################################################################################
                    log.debug("   Is there any execService that depends on [" + execService.getId() + "] in \"PROCESSING\" state?");
                    try {
                        dependantServices = dependenciesResolver.listDependantServices(execService);
                    } catch (ServiceNotExistsException e) {
                        log.error(e.toString());
                    } catch (InternalErrorException e) {
                        log.error(e.toString());
                    } catch (PrivilegeException e) {
                        log.error(e.toString());
                    } catch (Exception e) {
                        log.error(e.toString());
                    }
                    boolean proceed = true;
                    for (ExecService dependantService : dependantServices) {
                        Task dependantServiceTask = schedulingPool.getTask(dependantService, facility);
                        if (dependantServiceTask != null) {
                            if (dependantServiceTask.getStatus().equals(TaskStatus.PROCESSING)) {
                                log.debug("   There is a service [" + dependantService + "] running that depends on this one [" + execService + "], so we put this to sleep...");
                                //schedulingPool.addToPool(new Pair<ExecService, Facility>(execService, facility));
                                proceed = false;
                                break;
                            }
                        }
                    }
                    if (proceed) {
                        log.debug("   No, it is not. No dependent service is running, we can proceed.");
                        // If it is an ExecService of type SEND, we have to check its dependencies.
                        // We can skip this for GENERATE type (it has no dependencies by design).
                        // ########################################################################
                        log.debug("   Check whether the execService [" + execService.getId() + "] is of type SEND");
                        if (execService.getExecServiceType().equals(ExecServiceType.SEND)) {
                            log.debug("   Well, it is, so we have to check it's dependencies.");
                            // We check the status of all the ExecServices this ExecService depends on.
                            //
                            //
                            // Current approach disregards any SEND/GENERATE differences.
                            // Dependency on a GENERATE service is being treated as same as any other SEND dependency
                            // but for a small exception regarding ERROR and DONE states, see below:
                            //
                            //If the dependency is in one of the following states, we do:
                            //     NONE        Schedule it and wait (put this [ExecService,Facility] pair back to the SchedulingPool for a while).
                            //     PROCESSING  Wait
                            //     ERROR       IF dependency is GENERATE THEN DO
                            //                   Schedule it and wait (put this [ExecService,Facility] pair back to the SchedulingPool for a while).
                            //                 ELSE IF dependency is SEND THEN DO
                            //                   End with ERROR. (no point in trying, something is probably amiss on destination nodes...)
                            //                 ELSE
                            //                   throw new IllegalArgumentException
                            //                 FI
                            //     DONE        IF dependency is GENERATE THEN DO
                            //                   Schedule it and wait (put this [ExecService,Facility] pair back to the SchedulingPool for a while).
                            //                  
                            //                   It might look like we get an infinite loop where GENERATE will be in DONE and then rescheduled again and again.              
                            //                   It is not so because PropagationMaintainer sets its state to NONE as soon as the SEND, that depends on it,
                            //                   enters either DONE or ERROR states (one of its finite states).
                            //                 ELSE IF dependency is SEND THEN DO
                            //                   Proceed (Yes, no need to schedule this dependency, it is done already and we don't care for how long it has been so at this point.)
                            //                 ELSE
                            //                   throw new IllegalArgumentException
                            //                 FI 
                            // :-)
                            // #######################################################################################################
                            proceed = true;
                            try {
                                dependencies = dependenciesResolver.listDependencies(execService);
                                log.debug("listDependencies #1:" + dependencies);
                            } catch (ServiceNotExistsException e) {
                                log.error("listDependencies: ERROR execService[" + execService.getId() + "], service[" + execService.getService().getId() + "]: " + e.toString());
                            } catch (InternalErrorException e) {
                                log.error("listDependencies: ERROR" + e.toString());
                            } catch (PrivilegeException e) {
                                log.error("listDependencies: ERROR" + e.toString());
                            }
                            log.debug("   We are about to loop over execService [" + execService.getId() + "] dependencies.");
                            log.debug("listDependencies #2:" + dependencies);
                            log.debug("   Number of dependencies:" + dependencies);
                            for (ExecService dependency : dependencies) {
                                Task dependencyServiceTask = schedulingPool.getTask(dependency, facility);
                                if (dependencyServiceTask == null) {
                                    //Dependency being NULL is equivalent to being in NONE state.
                                    log.info("   Last Task [dependency:" + dependency.getId() + ", facility:" + facility.getId() + "] was NULL, we are gonna propagate.");
                                    scheduleItAndWait(dependency, facility, execService);
                                    proceed = false;
                                } else {
                                    switch (dependencyServiceTask.getStatus()) {
                                    case DONE:
                                        switch (dependency.getExecServiceType()) {
                                        case GENERATE:
                                            log.debug("   Dependency ID " + dependency.getId() + " is in DONE and it is of type GENERATE, we can proceed.");
                                            // Nothing, we can proceed...
                                            break;
                                        case SEND:
                                            log.debug("   Dependency ID " + dependency.getId() + " is in DONE and it is of type SEND, we can proceed.");
                                            // Nothing, we can proceed...
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Unknown ExecService type. Expected GENERATE or SEND.");
                                        }
                                        break;
                                    case ERROR:
                                        switch (dependency.getExecServiceType()) {
                                        case GENERATE:
                                            log.info("   Dependency ID " + dependency.getId() + " is in ERROR and it is of type GENERATE, we are gonna propagate.");
                                            scheduleItAndWait(dependency, facility, execService);
                                            proceed = false;
                                            break;
                                        case SEND:
                                            log.info("   Dependency ID " + dependency.getId() + " is in ERROR and it is of type SEND, we are gonna end with ERROR.");
                                            proceed = false;
                                            // We end Task with error immediately.
                                            task.setStatus(TaskStatus.ERROR);
                                            manipulateTasks(execService, facility, task);

                                            // And we set all its GENERATE dependencies as "dirty" by switching them to NONE state.
                                            // Note: Yes, there might have been some stored from the previous runs...
                                            propagationMaintainer.setAllGenerateDependenciesToNone(dependencies, facility);
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Unknown ExecService type. Expected GENERATE or SEND.");
                                        }
                                        break;
                                    case NONE:
                                        log.info("   Last Task [dependency:" + dependency.getId() + ", facility:" + facility.getId() + "] was NONE, we are gonna propagate.");
                                        scheduleItAndWait(dependency, facility, execService);
                                        proceed = false;
                                        break;
                                    case PLANNED:
                                        log.info("   Dependency ID " + dependency.getId() + " is in PLANNED so we are gonna wait.");
                                        // we do not need to put it back in pool here
                                        //justWait(facility, execService);
                                        proceed = false;
                                        break;
                                    case PROCESSING:
                                        log.info("   Dependency ID " + dependency.getId() + " is in PROCESSING so we are gonna wait.");
                                        // we do not need to put it back in pool here
                                        //justWait(facility, execService);
                                        proceed = false;
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Unknown Task status. Expected DONE, ERROR, NONE, PLANNED or PROCESSING.");
                                    }
                                }
                            }
                            // Finally, if we can proceed, we proceed...
                            // #########################################
                            if (proceed) {
                                log.info("   SCHEDULING execService [" + execService.getId() + "] facility [" + facility.getId() + "] as PLANNED.");
                                task.setSchedule(time);
                                task.setStatus(TaskStatus.PLANNED);
                                manipulateTasks(execService, facility, task);
                            } else {
                                // If we can not proceed, we just end here.
                                // ########################################
                                //The current ExecService,Facility pair should be sleeping in SchedulingPool at the moment...
                            }
                        } else if (execService.getExecServiceType().equals(ExecServiceType.GENERATE)) {
                            log.debug("   Well, it is not. ExecService of type GENERATE does not have any dependencies by design, so we schedule it immediately.");
                            log.info("   SCHEDULING execService [" + execService.getId() + "] facility [" + facility.getId() + "] as PLANNED.");
                            task.setSchedule(time);
                            task.setStatus(TaskStatus.PLANNED);
                            manipulateTasks(execService, facility, task);
                        } else {
                            throw new IllegalArgumentException("Unknown ExecService type. Expected GENERATE or SEND.");
                        }
                    } else {
                        log.debug("   We do not proceed, we put the [" + execService.getId() + "] execService to sleep.");
                    }
                } else {
                    log.debug("   The previous status is \"PROCESSING\". We won't do anything.");
                }
            } else {
                log.debug("   Yes, the execService ID:" + execService.getId() + " is denied on facility ID:" + facility.getId() + "?");
            }
        } else {
            log.debug("   No, execService ID:" + execService.getId() + " is not enabled globally.");
        }
		
	}

	private void scheduleItAndWait(ExecService dependency, Facility facility,
			ExecService execService) {
		Task task = new Task();
		task.setExecService(dependency);
		task.setFacility(facility);
		task.setStatus(TaskStatus.NONE);
		schedulingPool.addToPool(task);
	}

	@Override
	public int getPoolSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public SchedulingPool getSchedulingPool() {
		return schedulingPool;
	}

	public void setSchedulingPool(SchedulingPool schedulingPool) {
		this.schedulingPool = schedulingPool;
	}

}
