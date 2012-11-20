package org.bravo5.jenkins.vertx;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import org.easymock.EasyMock;
import org.easymock.Capture;
import org.easymock.IAnswer;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;

import java.util.Collections;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jenkins.model.Jenkins;
import hudson.model.Queue;
import hudson.model.Action;
import hudson.model.queue.CauseOfBlockage;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class EventBusQueueTaskDispatcherTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private EventBusQueueTaskDispatcher dispatcher;
    
    private EventBus mockEventBus;
    private Jenkins mockJenkins;

    // {{{ setUp
    @Before
    public void setUp() {
        mockEventBus = EasyMock.createMock("eventBus", EventBus.class);
        mockJenkins = EasyMock.createMock("jenkins", Jenkins.class);

        dispatcher = new EventBusQueueTaskDispatcher();

        dispatcher.setJenkins(mockJenkins);
        dispatcher.setEventBus(mockEventBus);
    }
    // }}}

    // {{{ tearDown
    @After
    public void tearDown() {
        dispatcher = null;

        mockEventBus = null;
        mockJenkins = null;
    }
    // }}}

    // {{{ initRegistersHandler
    @Test
    public void initRegistersHandler() {
        mockEventBus.registerHandler(
            eq("jenkins.queueTaskDispatcher"),
            eq(dispatcher)
        );

        replay(mockEventBus);

        dispatcher.init();

        verify(mockEventBus);
    }
    // }}}

    // {{{ closeUnregistersHandler
    @Test
    public void closeUnregistersHandler() {
        mockEventBus.unregisterHandler(
            eq("jenkins.queueTaskDispatcher"),
            eq(dispatcher)
        );

        replay(mockEventBus);

        dispatcher.close();

        verify(mockEventBus);
    }
    // }}}

    // {{{ registersSingleHandler
    /**
     * Register a single handler, verify the reply message.
     */
    @Test
    public void registersSingleHandler() {
        final Message<JsonObject> msg = EasyMock.createMock(Message.class);
        Capture<JsonObject> replyCap = new Capture<>();

        msg.body = new JsonObject()
            .putString("action", "register")
            .putString("handlerAddress", "someAddr");

        msg.reply(capture(replyCap));

        replay(msg, mockEventBus);

        // invoke the handler
        dispatcher.handle(msg);

        verify(msg, mockEventBus);

        assertTrue(replyCap.hasCaptured());

        assertEquals("ok", replyCap.getValue().getString("status"));
    }
    // }}}

    // {{{ invokesHandlerOnCanRun
    /**
     * Invokes a registered handler when QueueTaskDispatcher#canRun() is called.
     */
    @Test
    public void invokesHandlerOnCanRun() {
        registersSingleHandler();

        reset(mockEventBus);

        Queue.Task mockQueueTask =
            EasyMock.createNiceMock("task", Queue.Task.class);

        Queue.WaitingItem queueItem = new Queue.WaitingItem(
            Calendar.getInstance(),
            mockQueueTask,
            Collections.<Action>emptyList()
        );

        Capture<JsonObject> payloadCap = new Capture<>();

        mockEventBus.send(
            eq("someAddr"),
            capture(payloadCap),
            isA(Handler.class)
        );

        // invoke the handler passed to send()
        expectLastCall()
            .andAnswer(new IAnswer<Void>() {
                public Void answer() {
                    Handler<Message<JsonObject>> handler =
                        (Handler<Message<JsonObject>>) getCurrentArguments()[2];

                    Message<JsonObject> resultMsg = EasyMock.createMock(Message.class);
                    resultMsg.body = new JsonObject().putBoolean("canRun", false);

                    handler.handle(resultMsg);

                    return null; // le sigh
                }
            });

        replay(mockQueueTask, mockEventBus);

        CauseOfBlockage cause = dispatcher.canRun(queueItem);

        verify(mockQueueTask, mockEventBus);

        assertNotNull(cause);
        assertEquals("reason not specified", cause.getShortDescription());
    }
    // }}}

    // {{{ canRunWithoutHandlerReturnsNull
    /**
     * Sane default behavior when no handler is registered.
     */
    @Test
    public void canRunWithoutHandlerReturnsNull() {
        Queue.Task mockQueueTask =
            EasyMock.createMock("task", Queue.Task.class);

        Queue.WaitingItem queueItem = new Queue.WaitingItem(
            Calendar.getInstance(),
            mockQueueTask,
            Collections.<Action>emptyList()
        );

        replay(mockQueueTask, mockEventBus);

        CauseOfBlockage cause = dispatcher.canRun(queueItem);

        verify(mockQueueTask, mockEventBus);

        assertNull(cause);
    }
    // }}}
}
