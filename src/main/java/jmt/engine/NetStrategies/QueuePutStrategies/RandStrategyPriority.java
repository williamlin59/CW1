package jmt.engine.NetStrategies.QueuePutStrategies;

import java.util.List;
import java.util.ListIterator;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NodeSections.Server;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;

public class RandStrategyPriority implements QueuePutStrategy{

	/**
	 * Arriving jobs are put in the Queue randomly but priority is also taken into account. 
	 * @param job Job to be added to the queue.
	 * @param queue Queue.
	 * @param sourceSection Job source section.
	 * @param sourceNode Job source node.
	 * @param callingSection The section which calls this strategy.
	 */
	
	public void put(Job job, JobInfoList queue, byte sourceSection,
			NetNode sourceNode, NodeSection callingSection) throws NetException {
		
	Server server = (Server) callingSection.getOwnerNode().getSection(NodeSection.SERVICE);
		
		ServiceStrategy[] strategy = server.getServiceStrategy();
					
		double serviceTime = strategy[job.getJobClass().getId()].wait(server);
		
		job.setServiceTime(serviceTime); 
		
		//priority of this job
		int priority = job.getJobClass().getPriority();

		//list of jobs in queue
		List<JobInfo> list = queue.getJobList();

		if (list.size() == 0) {
			//empty list: add first in general and class lists
			queue.addFirst(new JobInfo(job), false);
			return;
		}

		//else creates an iterator and find the correct position
		//according to the job priority

		ListIterator<JobInfo> iterator = list.listIterator();
		JobInfo current = null;
		int currentJobPriority = 0;
		int index = -1;
		int firstElement = -1, lastElement = -1;
		int lastHigh = -1, lastLow = -1;

		//iterator starts from the first (i.e. the job with highest priority)
		while (iterator.hasNext()) {
			index++;
			current = iterator.next();
			currentJobPriority = current.getJob().getJobClass().getPriority();
			
			switch (currentJobPriority){
				case 1: lastLow = index; break;
				case 3: lastHigh = index; break;
			}

			if (priority == currentJobPriority) {
			
				firstElement = index;
				while (iterator.hasNext() && iterator.next().getJob().getJobClass().getPriority()==priority){
					index++;
				}
				lastElement = index;
				
				// It will add the job randomly in the particular priority it belongs
				
				int pos = (int) (firstElement + (int)(Math.random() * (lastElement - firstElement)));
				
				queue.add(pos, new JobInfo(job), false);
				
				return;
			}
			//else if priority is not equal than current, continue iteration
		}

		/* This appears to be a leftover, I commented it (note by G. Casale, July 2015) 
		switch (priority){
			case 1: queue.addLast(new JobInfo(job), false); break;
			case 2: if (lastLow == -1) {
							queue.addLast(new JobInfo(job), false);
					}
					else queue.add(lastHigh+1,new JobInfo(job), false); break;
			case 3: queue.addFirst(new JobInfo(job), false); break;
			
		}
		*/
		
		return;
	}
	
	/* (non-Javadoc)
	 * @see jmt.common.AutoCheck#check()
	 */	

	public boolean check() {
		
		return true;
	}

}
