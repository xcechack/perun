package cz.metacentrum.perun.dispatcher.unit;

import java.util.Collection;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import cz.metacentrum.perun.dispatcher.TestBase;
import cz.metacentrum.perun.dispatcher.TestDataSourcePopulator;
import cz.metacentrum.perun.dispatcher.jms.DispatcherQueue;
import cz.metacentrum.perun.dispatcher.model.Event;
import cz.metacentrum.perun.dispatcher.processing.EventLogger;
import cz.metacentrum.perun.dispatcher.processing.EventQueue;
import cz.metacentrum.perun.dispatcher.processing.SmartMatcher;
import cz.metacentrum.perun.dispatcher.processing.impl.EventProcessorImpl;

public class EventProcessorTest extends TestBase {
	private final static Logger log = LoggerFactory.getLogger(EventProcessorTest.class);

	@Autowired
	private TestDataSourcePopulator testDataPopulator;	

	@Autowired
	private EventProcessorImpl eventProcessor;

	private EventProcessorImpl.EvProcessor evProcessor;
	private String messageSent;
	
	private class EventQueueMock implements EventQueue {
		private boolean eventConsumed = false;
		
		@Override
		public void add(Event event) {
		}

		@Override
		public Event poll() {
			if(eventConsumed) {
				return null;
			}
			Event event = new Event();
			event.setTimeStamp(System.currentTimeMillis());
			event.setHeader("portishead");
			event.setData(testDataPopulator.getMember1().serializeToString() + " added to " + testDataPopulator.getGroup1().serializeToString() + ".");
			eventConsumed = true;
			return event;
		}

		@Override
		public int size() {
			if(eventConsumed) {
				return 0; 
			} else {
				return 1;
			}
		}
		
	}
	private class DispatcherQueueMock extends DispatcherQueue {

		public DispatcherQueueMock(int clientID, String queueName) {
			super(clientID, queueName);
		}

		@Override
		@Async
		public void sendMessage(String text) {
			messageSent = text;
			evProcessor.stop();
		}
		
	}
	
	@Test(timeout=1000)
	public void eventProcessorTest() {
		DispatcherQueue dispatcherQueue = new DispatcherQueueMock(1, "testQueue");
		eventProcessor.getDispatcherQueuePool().addDispatcherQueue(dispatcherQueue);
		eventProcessor.setEventQueue(new EventQueueMock());
		eventProcessor.getSmartMatcher().loadAllRulesFromDB();
		evProcessor = eventProcessor.new EvProcessor();
		// runs inside this thread, should end when message is delivered
		// this necessitates the use of test timeout
		evProcessor.run();
		log.debug("Event sent: " + messageSent);
	}
}
