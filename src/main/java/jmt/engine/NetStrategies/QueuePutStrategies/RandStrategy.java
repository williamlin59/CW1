package jmt.engine.NetStrategies.QueuePutStrategies;
import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NodeSections.Server;
import jmt.engine.QueueNet.*;

/** @author Anastasia Eleftheriou
 * 
 * 
 */

public class RandStrategy implements QueuePutStrategy{
	/**
	 * arriving jobs are randomly put in the Queue.
	 * @param job Job to be added to the queue.
	 * @param queue Queue.
	 * @param sourceSection Job source section.
	 * @param sourceNode Job source node.
	 * @param callingSection The section which calls this strategy.
	 * @throws NetException 
	 */
	public void put(Job job, JobInfoList queue, byte sourceSection, NetNode sourceNode, NodeSection callingSection) throws NetException {
		
	Server server = (Server) callingSection.getOwnerNode().getSection(NodeSection.SERVICE);
		
		ServiceStrategy[] strategy = server.getServiceStrategy();
					
		double serviceTime = strategy[job.getJobClass().getId()].wait(server);
		
		//double serviceTime = Math.random(); //for testing purposes
		job.setServiceTime(serviceTime); 
		queue.addRand(new JobInfo(job));
	}
	
	/* (non-Javadoc)
	 * @see jmt.common.AutoCheck#check()
	 */	
	public boolean check() {
		return true;
	}
}
