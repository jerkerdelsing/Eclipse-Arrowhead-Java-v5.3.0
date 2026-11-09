/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.common.mqtt.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttResourceManager;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.MqttRequestTemplate;

@ExtendWith(MockitoExtension.class)
public class MqttTopicHandlerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TestMqttHandler handler;

	@Mock
	private Function<MqttMessageContainerHandlerContext, MqttMessageContainerHandler> messageHandlerFactory;

	@Mock
	private MqttHandlerUtils utils;

	@Mock
	private List<ArrowheadMqttFilter> filters;

	// these seems to be necessary to inject mocks into a Thread-descendant class

	@Mock
	private Object scopedValueBindings;

	@Mock
	private Object parkBlocker;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInit() {
		assertNull(ReflectionTestUtils.getField(handler, "queue"));
		assertNull(ReflectionTestUtils.getField(handler, "threadpool"));

		doNothing().when(filters).sort(any(Comparator.class));

		final BlockingQueue<MqttMessageContainer> testQueue = new LinkedBlockingQueue<>();

		assertDoesNotThrow(() -> handler.init(testQueue));

		assertEquals(testQueue, ReflectionTestUtils.getField(handler, "queue"));
		assertNotNull(ReflectionTestUtils.getField(handler, "threadpool"));
		verify(filters).sort(any(Comparator.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitComparator() {
		final DummyArrowheadMqttFilter filterFirst = new DummyArrowheadMqttFilter(10);
		final DummyArrowheadMqttFilter filterSecond = new DummyArrowheadMqttFilter(20);
		final List<ArrowheadMqttFilter> testFilterList = new ArrayList<>(2);
		testFilterList.add(filterSecond);
		testFilterList.add(filterFirst);

		ReflectionTestUtils.setField(handler, "filters", testFilterList);

		final BlockingQueue<MqttMessageContainer> testQueue = new LinkedBlockingQueue<>();

		assertDoesNotThrow(() -> handler.init(testQueue));

		final List<ArrowheadMqttFilter> sorted = (List<ArrowheadMqttFilter>) ReflectionTestUtils.getField(handler, "filters");
		assertEquals(filterFirst, sorted.get(0));
		assertEquals(filterSecond, sorted.get(1));

		ReflectionTestUtils.setField(handler, "filters", filters);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInterrupt() {
		ReflectionTestUtils.setField(handler, "doWork", true);

		assertDoesNotThrow(() -> handler.interrupt());
		final boolean doWork = (boolean) ReflectionTestUtils.getField(handler, "doWork");
		assertFalse(doWork);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunNotInitialized() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> handler.run());

		assertEquals("eu.arrowhead.common.mqtt.handler.MqttTopicHandlerTest$TestMqttHandler is not initialized", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunCanFinished() {
		final BlockingQueue<MqttMessageContainer> testQueue = new LinkedBlockingQueue<>();
		testQueue.add(new MqttMessageContainer("test", new MqttMessage()));

		doNothing().when(filters).sort(any(Comparator.class));
		when(messageHandlerFactory.apply(any(MqttMessageContainerHandlerContext.class))).thenAnswer(invocation -> {
			final MqttMessageContainerHandlerContext context = (MqttMessageContainerHandlerContext) invocation.getArgument(0);

			ReflectionTestUtils.setField(handler, "doWork", false);

			return new DummyMqttMessageContainerHandler(
					context.topicHandler(),
					context.msgContainer(),
					context.resourceManager());
		});

		handler.init(testQueue);
		assertDoesNotThrow(() -> handler.run());

		verify(filters).sort(any(Comparator.class));
		verify(messageHandlerFactory).apply(any(MqttMessageContainerHandlerContext.class));

		final boolean doWork = (boolean) ReflectionTestUtils.getField(handler, "doWork");
		assertFalse(doWork);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunInterrupted() {
		final BlockingQueue<MqttMessageContainer> testQueue = new LinkedBlockingQueue<>();
		testQueue.add(new MqttMessageContainer("test", new MqttMessage()));

		doNothing().when(filters).sort(any(Comparator.class));
		when(messageHandlerFactory.apply(any(MqttMessageContainerHandlerContext.class))).thenAnswer(invocation -> {
			ReflectionTestUtils.setField(handler, "doWork", false);
			throw new InterruptedException();
		});

		handler.init(testQueue);
		assertDoesNotThrow(() -> handler.run());

		verify(filters).sort(any(Comparator.class));
		verify(messageHandlerFactory).apply(any(MqttMessageContainerHandlerContext.class));

		final boolean doWork = (boolean) ReflectionTestUtils.getField(handler, "doWork");
		assertFalse(doWork);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunRejectedExecutionException() {
		final BlockingQueue<MqttMessageContainer> testQueue = new LinkedBlockingQueue<>();
		testQueue.add(new MqttMessageContainer("test", new MqttMessage()));

		doNothing().when(filters).sort(any(Comparator.class));
		handler.init(testQueue);

		final ThreadPoolExecutor realThreadpool = (ThreadPoolExecutor) ReflectionTestUtils.getField(handler, "threadpool");
		final LoveToRejectThreadpoolExecutor testThreadpool = new LoveToRejectThreadpoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		ReflectionTestUtils.setField(handler, "threadpool", testThreadpool);

		when(utils.parseMqttMessage(any(MqttMessageContainer.class))).thenAnswer(invocation -> {
			ReflectionTestUtils.setField(handler, "doWork", false);
			throw new InvalidParameterException("test invalid parameter exception");
		});
		doNothing().when(utils).errorResponse(any(RejectedExecutionException.class), isNull());

		assertDoesNotThrow(() -> handler.run());

		verify(filters).sort(any(Comparator.class));
		verify(utils).parseMqttMessage(any(MqttMessageContainer.class));
		verify(utils).errorResponse(any(RejectedExecutionException.class), isNull());

		final boolean doWork = (boolean) ReflectionTestUtils.getField(handler, "doWork");
		assertFalse(doWork);

		ReflectionTestUtils.setField(handler, "threadpool", realThreadpool);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunRejectedExecutionException2() {
		final BlockingQueue<MqttMessageContainer> testQueue = new LinkedBlockingQueue<>();
		testQueue.add(new MqttMessageContainer("test", new MqttMessage()));

		doNothing().when(filters).sort(any(Comparator.class));
		handler.init(testQueue);

		final ThreadPoolExecutor realThreadpool = (ThreadPoolExecutor) ReflectionTestUtils.getField(handler, "threadpool");
		final LoveToRejectThreadpoolExecutor testThreadpool = new LoveToRejectThreadpoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		ReflectionTestUtils.setField(handler, "threadpool", testThreadpool);

		when(utils.parseMqttMessage(any(MqttMessageContainer.class))).thenAnswer(invocation -> {
			ReflectionTestUtils.setField(handler, "doWork", false);

			return new ImmutablePair<String, MqttRequestModel>(
					"test",
					new MqttRequestModel(
							"testBaseTopic",
							"test-operation",
							new MqttRequestTemplate("traceId", null, "response", 0, null, null)));
		});
		doNothing().when(utils).errorResponse(any(RejectedExecutionException.class), any(MqttRequestModel.class));

		assertDoesNotThrow(() -> handler.run());

		verify(filters).sort(any(Comparator.class));
		verify(utils).parseMqttMessage(any(MqttMessageContainer.class));
		verify(utils).errorResponse(any(RejectedExecutionException.class), any(MqttRequestModel.class));

		final boolean doWork = (boolean) ReflectionTestUtils.getField(handler, "doWork");
		assertFalse(doWork);

		ReflectionTestUtils.setField(handler, "threadpool", realThreadpool);
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static final class TestMqttHandler extends MqttTopicHandler {

		//-------------------------------------------------------------------------------------------------
		@Override
		public String baseTopic() {
			return "test/";
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void handle(final MqttRequestModel request) throws ArrowheadException {
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static final class DummyArrowheadMqttFilter implements ArrowheadMqttFilter {

		private final int order;

		//-------------------------------------------------------------------------------------------------
		public DummyArrowheadMqttFilter(final int order) {
			this.order = order;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public int order() {
			return order;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void doFilter(final String authKey, final MqttRequestModel request) {
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static final class DummyMqttMessageContainerHandler extends MqttMessageContainerHandler {

		//-------------------------------------------------------------------------------------------------
		public DummyMqttMessageContainerHandler(final MqttTopicHandler topicHandler, final MqttMessageContainer msgContainer, final MqttResourceManager resourceManager) {
			super(topicHandler, msgContainer, resourceManager);
		}

		//-------------------------------------------------------------------------------------------------
		@SuppressWarnings("checkstyle:MagicNumber")
		public void run() {
			try {
				Thread.sleep(100);
			} catch (final InterruptedException ex) {
				// intentionally blank
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static final class LoveToRejectThreadpoolExecutor extends ThreadPoolExecutor {

		//-------------------------------------------------------------------------------------------------
		public LoveToRejectThreadpoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void execute(final Runnable command) {
			throw new RejectedExecutionException("test");
		}

	}
}