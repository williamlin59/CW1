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
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.Parameter;

public class LJFStrategy implements QueuePutStrategy{
	
	/**
	 * arriving jobs are put in the Queue in a Longest-Job-First manner.
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
		
		//double serviceTime = Math.random(); //for testing purposes
		job.setServiceTime(serviceTime);
			
		//list of jobs in queue
		List<JobInfo> list = queue.getJobList();

		if (list.size() == 0) {
			//empty list: add first in general and class lists
			queue.addFirst(new JobInfo(job), false);
			return;
		}

		//else creates an iterator and find the correct position
		//according to the job size

		ListIterator<JobInfo> iterator = list.listIterator();
		JobInfo current = null;
		int index = -1;

		//iterator starts from the first (i.e. the job with highest priority)
		while (iterator.hasNext()) {
			index++;
			current = iterator.next();
			double currentServiceTime = current.getJob().getServiceTime();
			
			if (serviceTime >= currentServiceTime) {
				//the job to be added must be inserted before the current job
				//index is the position of the current element, which will be shifted together
				//with the following ones

				queue.add(index, new JobInfo(job), false);
				return;
			}
			
			//else if Service Time is longer than current, continue iteration
		}

		//exiting from the "while" means that the job to be inserted is the job with
		//longest size
		//add last
		queue.addLast(new JobInfo(job), false);
		return;
		
		
	}

	
	/* (non-Javadoc)
	 * @see jmt.common.AutoCheck#check()
	 */	
	public boolean check() {
		return true;
	}

}

