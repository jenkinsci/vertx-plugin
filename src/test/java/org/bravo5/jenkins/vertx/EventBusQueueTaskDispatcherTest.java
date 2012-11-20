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

    private static final String HANDLER_CALLBACK_ID = "someAddr";

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

    // {{{ unknownActionReturnsError
    @Test
    public void unknownActionReturnsError() {
        final Message<JsonObject> msg = EasyMock.createMock(Message.class);
        Capture<JsonObject> replyCap = new Capture<>();

        msg.body = new JsonObject()
            .putString("action", "someInvalidAction")
            .putString("foo", "bar");

        msg.reply(capture(replyCap));

        replay(msg, mockEventBus);

        // invoke the handler
        dispatcher.handle(msg);

        verify(msg, mockEventBus);

        checkError(replyCap, "unknown action someInvalidAction");
    }
    // }}}

    // {{{ missingActionReturnsError
    @Test
    public void missingActionReturnsError() {
        final Message<JsonObject> msg = EasyMock.createMock(Message.class);
        Capture<JsonObject> replyCap = new Capture<>();

        msg.body = new JsonObject()
            .putString("foo", "bar");

        msg.reply(capture(replyCap));

        replay(msg, mockEventBus);

        // invoke the handler
        dispatcher.handle(msg);

        verify(msg, mockEventBus);

        checkError(replyCap, "no action provided");
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
            .putString("handlerAddress", HANDLER_CALLBACK_ID);

        msg.reply(capture(replyCap));

        replay(msg, mockEventBus);

        // invoke the handler
        dispatcher.handle(msg);

        verify(msg, mockEventBus);

        checkOk(replyCap);
    }
    // }}}

    // {{{ unregistersPreviouslyRegisteredHandler
    @Test
    public void unregistersPreviouslyRegisteredHandler() {
        registersSingleHandler();

        reset(mockEventBus);

        final Message<JsonObject> msg = EasyMock.createMock(Message.class);
        Capture<JsonObject> replyCap = new Capture<>();

        // the unregister message
        msg.body = new JsonObject()
            .putString("action", "unregister")
            .putString("handlerAddress", HANDLER_CALLBACK_ID);

        // prepare to capture the reply
        msg.reply(capture(replyCap));

        replay(msg, mockEventBus);

        // invoke the handler
        dispatcher.handle(msg);

        verify(msg, mockEventBus);

        checkOk(replyCap);
    }
    // }}}

    // {{{ handlesUnregisteringIncorrectHandlerId
    @Test
    public void handlesUnregisteringIncorrectHandlerId() {
        final Message<JsonObject> msg = EasyMock.createMock(Message.class);
        Capture<JsonObject> replyCap = new Capture<>();

        // the unregister message
        msg.body = new JsonObject()
            .putString("action", "unregister")
            .putString("handlerAddress", "someInvalidAddr");

        // prepare to capture the reply
        msg.reply(capture(replyCap));

        replay(msg, mockEventBus);

        // invoke the handler
        dispatcher.handle(msg);

        verify(msg, mockEventBus);

        checkError(replyCap, "handler ID mismatch");
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
            eq(HANDLER_CALLBACK_ID),
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
                    resultMsg.body = new JsonObject()
                        .putBoolean("canRun", false)
                        .putString("reason", "'cause we said so");

                    handler.handle(resultMsg);

                    return null; // le sigh
                }
            });

        replay(mockQueueTask, mockEventBus);

        CauseOfBlockage cause = dispatcher.canRun(queueItem);

        verify(mockQueueTask, mockEventBus);

        assertNotNull(cause);
        assertEquals("'cause we said so", cause.getShortDescription());
    }
    // }}}

    // {{{ explicitPositiveCanRunWorks
    @Test
    public void explicitPositiveCanRunWorks() {
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
            eq(HANDLER_CALLBACK_ID),
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
                    resultMsg.body = new JsonObject().putBoolean("canRun", true);

                    handler.handle(resultMsg);

                    return null; // le sigh
                }
            });

        replay(mockQueueTask, mockEventBus);

        CauseOfBlockage cause = dispatcher.canRun(queueItem);

        verify(mockQueueTask, mockEventBus);

        assertNull(cause);
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

    // {{{ handlesTimeoutInCallbackGracefully
    /**
     * If a registered handler takes longer than the pre-determined time, we
     * should abort and return null, allowing the queued item to proceed.
     */
    @Test
    public void handlesTimeoutInCallbackGracefully() {
        registersSingleHandler();

        reset(mockEventBus);

        // reduce the timeout to something short for the tests
        dispatcher.setTimeoutMillis(30);

        Queue.Task mockQueueTask =
            EasyMock.createNiceMock("task", Queue.Task.class);

        Queue.WaitingItem queueItem = new Queue.WaitingItem(
            Calendar.getInstance(),
            mockQueueTask,
            Collections.<Action>emptyList()
        );

        mockEventBus.send(
            eq(HANDLER_CALLBACK_ID),
            isA(JsonObject.class),
            isA(Handler.class)
        );

        // instead of returning a response, just sleep for just longer than the
        // configured timeout
        expectLastCall()
            .andAnswer(new IAnswer<Void>() {
                public Void answer() {
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException e) {
                        fail("interrupted sleep");
                    }

                    return null; // le sigh
                }
            });

        replay(mockQueueTask, mockEventBus);

        CauseOfBlockage cause = dispatcher.canRun(queueItem);

        verify(mockQueueTask, mockEventBus);

        assertNull(cause);
    }
    // }}}

    // {{{ checkOk
    private void checkOk(final Capture<JsonObject> cap) {
        assertTrue(cap.hasCaptured());
        
        assertEquals("ok", cap.getValue().getString("status"));
    }
    // }}}

    // {{{ checkError
    private void checkError(final Capture<JsonObject> cap, final String msg) {
        assertTrue(cap.hasCaptured());

        assertEquals("error", cap.getValue().getString("status"));
        assertEquals(msg, cap.getValue().getString("message"));
    }
    // }}}
}
