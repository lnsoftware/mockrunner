package com.mockrunner.test.jms;

import javax.jms.ConnectionConsumer;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockQueueConnection;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockQueueReceiver;
import com.mockrunner.mock.jms.MockQueueSender;
import com.mockrunner.mock.jms.MockQueueSession;
import com.mockrunner.mock.jms.MockTopic;
import com.mockrunner.mock.jms.MockTopicConnection;
import com.mockrunner.mock.jms.MockTopicConnectionFactory;
import com.mockrunner.mock.jms.MockTopicPublisher;
import com.mockrunner.mock.jms.MockTopicSession;
import com.mockrunner.mock.jms.MockTopicSubscriber;

import junit.framework.TestCase;

public class MockConnectionTest extends TestCase
{
    private MockQueueConnection queueConnection;
    private MockTopicConnection topicConnection;
     
    protected void setUp() throws Exception
    {
        super.setUp();
        DestinationManager manager = new DestinationManager();
        queueConnection = new MockQueueConnection(manager);
        topicConnection = new MockTopicConnection(manager);
    }
    
    public void testFactories() throws Exception
    {
        MockQueueConnectionFactory queueFactory = new MockQueueConnectionFactory(new DestinationManager());
        MockQueueConnection queueConnection1 = (MockQueueConnection)queueFactory.createQueueConnection();
        MockQueueConnection queueConnection2 = (MockQueueConnection)queueFactory.createQueueConnection();
        JMSException exception = new JMSException("");
        queueFactory.setJMSException(exception);
        MockQueueConnection queueConnection3 = (MockQueueConnection)queueFactory.createQueueConnection("", "");
        assertEquals(queueConnection1, queueFactory.getQueueConnection(0));
        assertEquals(queueConnection2, queueFactory.getQueueConnection(1));
        assertEquals(queueConnection3, queueFactory.getQueueConnection(2));
        assertEquals(queueConnection3, queueFactory.getLatestQueueConnection());
        assertNull(queueFactory.getQueueConnection(3));
        queueConnection1.throwJMSException();
        queueConnection2.throwJMSException();
        try
        {
            queueConnection3.throwJMSException();
            fail();
        }
        catch(JMSException exc)
        {
            assertEquals(exc, exception);
        }
        MockTopicConnectionFactory topicFactory = new MockTopicConnectionFactory(new DestinationManager());
        topicFactory.setJMSException(exception);
        MockTopicConnection topicConnection1 = (MockTopicConnection)topicFactory.createTopicConnection(null, null);
        MockTopicConnection topicConnection2 = (MockTopicConnection)topicFactory.createTopicConnection();
        topicFactory.setJMSException(null);
        MockTopicConnection topicConnection3 = (MockTopicConnection)topicFactory.createTopicConnection();
        assertEquals(topicConnection1, topicFactory.getTopicConnection(0));
        assertEquals(topicConnection2, topicFactory.getTopicConnection(1));
        assertEquals(topicConnection3, topicFactory.getTopicConnection(2));
        assertEquals(topicConnection3, topicFactory.getLatestTopicConnection());
        assertNull(topicFactory.getTopicConnection(3));
        try
        {
            topicConnection1.throwJMSException();
            fail();
        }
        catch(JMSException exc)
        {
            assertEquals(exc, exception);
        }
        try
        {
            topicConnection2.throwJMSException();
            fail();
        }
        catch(JMSException exc)
        {
            assertEquals(exc, exception);
        }
        topicConnection3.throwJMSException();
    }
    
    public void testFactoriesCreateConnection() throws Exception
    {
        MockQueueConnectionFactory queueFactory = new MockQueueConnectionFactory(new DestinationManager());
        assertTrue(queueFactory.createConnection() instanceof QueueConnection);
        assertTrue(queueFactory.createConnection(null, null) instanceof QueueConnection);
        MockTopicConnectionFactory topicFactory = new MockTopicConnectionFactory(new DestinationManager());
        assertTrue(topicFactory.createConnection() instanceof TopicConnection);
        assertTrue(topicFactory.createConnection(null, null) instanceof TopicConnection);
    }
    
    public void testClose() throws Exception
    {
        MockQueueSession queueSession1 = (MockQueueSession)queueConnection.createQueueSession(true, QueueSession.CLIENT_ACKNOWLEDGE);
        MockQueueSession queueSession2 = (MockQueueSession)queueConnection.createQueueSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
        MockQueueSession queueSession3 = (MockQueueSession)queueConnection.createQueueSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
        MockQueueSender sender1 = (MockQueueSender)queueSession1.createSender(new MockQueue(""));
        MockQueueSender sender2 = (MockQueueSender)queueSession1.createSender(new MockQueue(""));
        MockQueueReceiver receiver1 = (MockQueueReceiver)queueSession1.createReceiver(new MockQueue(""));
        queueConnection.close();
        assertTrue(queueConnection.isClosed());
        assertTrue(queueSession1.isClosed());
        assertTrue(queueSession2.isClosed());
        assertTrue(queueSession3.isClosed());
        assertTrue(sender1.isClosed());
        assertTrue(sender2.isClosed());
        assertTrue(receiver1.isClosed());
        assertTrue(queueSession1.isRolledBack());
        assertFalse(queueSession2.isRolledBack());
        assertFalse(queueSession3.isRolledBack());
        assertTrue(queueSession1.isRecovered());
        assertFalse(queueSession2.isRecovered());
        assertFalse(queueSession3.isRecovered());
        MockTopicSession topicSession1 = (MockTopicSession)topicConnection.createTopicSession(false, QueueSession.CLIENT_ACKNOWLEDGE);
        MockTopicSession topicSession2 = (MockTopicSession)topicConnection.createTopicSession(true, QueueSession.CLIENT_ACKNOWLEDGE);
        MockTopicPublisher publisher1 = (MockTopicPublisher)topicSession2.createPublisher(new MockTopic(""));
        MockTopicSubscriber subscriber1 = (MockTopicSubscriber)topicSession2.createSubscriber(new MockTopic(""));
        MockTopicSubscriber subscriber2 = (MockTopicSubscriber)topicSession2.createSubscriber(new MockTopic(""));
        topicConnection.close();
        assertTrue(topicConnection.isClosed());
        assertTrue(topicSession1.isClosed());
        assertTrue(topicSession2.isClosed());
        assertTrue(publisher1.isClosed());
        assertTrue(subscriber1.isClosed());
        assertTrue(subscriber2.isClosed());
        assertFalse(topicSession1.isRolledBack());
        assertTrue(topicSession2.isRolledBack());
        assertFalse(topicSession1.isRecovered());
        assertTrue(topicSession2.isRecovered());
    }
    
    public void testCreateConsumerAndSession() throws Exception
    {
        assertTrue(queueConnection.createSession(true, QueueSession.CLIENT_ACKNOWLEDGE) instanceof QueueSession);
        assertNotNull(queueConnection.createConnectionConsumer((Destination)null, null, null, 0));
        assertNotNull(queueConnection.createConnectionConsumer((Queue)null, null, null, 0));
        assertNotNull(queueConnection.createDurableConnectionConsumer(null, null, null, null, 0));
        assertTrue(topicConnection.createSession(true, QueueSession.CLIENT_ACKNOWLEDGE) instanceof TopicSession);
        assertNotNull(topicConnection.createConnectionConsumer((Destination)null, null, null, 0));
        assertNotNull(topicConnection.createConnectionConsumer((Topic)null, null, null, 0));
        assertNotNull(queueConnection.createDurableConnectionConsumer(null, null, null, null, 0));
    }
    
    public void testCreateQueueSession() throws Exception
    {
        queueConnection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
        queueConnection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
        queueConnection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
        assertTrue(queueConnection.getSession(0) instanceof MockQueueSession);
        assertTrue(queueConnection.getQueueSession(0) instanceof MockQueueSession);
        assertTrue(queueConnection.getSession(1) instanceof MockQueueSession);
        assertTrue(queueConnection.getQueueSession(1) instanceof MockQueueSession);
        assertTrue(queueConnection.getSession(2) instanceof MockQueueSession);
        assertTrue(queueConnection.getQueueSession(2) instanceof MockQueueSession);
        assertNull(queueConnection.getSession(3));
        assertNull(queueConnection.getQueueSession(3));
        assertEquals(3, queueConnection.getSessionList().size());
        assertEquals(3, queueConnection.getQueueSessionList().size());
    }
    
    public void testCreateTopicSession() throws Exception
    {
        topicConnection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
        topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        topicConnection.createTopicSession(true, Session.CLIENT_ACKNOWLEDGE);
        assertTrue(topicConnection.getSession(0) instanceof MockTopicSession);
        assertTrue(topicConnection.getTopicSession(0) instanceof MockTopicSession);
        assertTrue(topicConnection.getSession(1) instanceof MockTopicSession);
        assertTrue(topicConnection.getTopicSession(1) instanceof MockTopicSession);
        assertTrue(topicConnection.getSession(2) instanceof MockTopicSession);
        assertTrue(topicConnection.getTopicSession(2) instanceof MockTopicSession);
        assertNull(topicConnection.getSession(3));
        assertNull(topicConnection.getTopicSession(3));
        assertEquals(3, topicConnection.getSessionList().size());
        assertEquals(3, topicConnection.getTopicSessionList().size());
    }

    public void testException() throws Exception
    {
        queueConnection.createQueueSession(true, QueueSession.CLIENT_ACKNOWLEDGE);
        JMSException exception = new JMSException("MyReason");
        TestExceptionListener listener = new TestExceptionListener();
        queueConnection.setExceptionListener(listener);
        queueConnection.setJMSException(exception);
        try
        {
            queueConnection.createQueueSession(true, QueueSession.CLIENT_ACKNOWLEDGE);
            fail();
        }
        catch(JMSException exc)
        {
            assertTrue(exception == exc);
            assertTrue(exception == listener.getException());
        }
        queueConnection.start();
        ConnectionConsumer consumer = queueConnection.createConnectionConsumer(null, null, null, 0);
        consumer.getServerSessionPool();
        exception = new JMSException("MyReason");
        queueConnection.setJMSException(exception);
        try
        {
            consumer.getServerSessionPool();
            fail();
        }
        catch(JMSException exc)
        {
            assertTrue(exception == exc);
            assertTrue(exception == listener.getException());
        }
        consumer.getServerSessionPool();
    }
    
    private static class TestExceptionListener implements ExceptionListener
    {
        private JMSException exception;
        
        public void onException(JMSException exception)
        {
            this.exception = exception;
        }
        
        public JMSException getException()
        {
            return exception;
        }
    }
}
