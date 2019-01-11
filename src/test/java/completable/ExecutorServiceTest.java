package completable;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Uninterruptibles;

class ExecutorServiceTest {

	@Test
	void invokeAllTest() throws InterruptedException, ExecutionException {
		ExecutorService pool = Executors.newCachedThreadPool();

		List<MyTask> tasks = Stream.generate(() -> new MyTask()).limit(10).collect(Collectors.toList());

		pool.invokeAll(tasks);
		assertTrue(tasks.stream().allMatch(t -> t.executed));

	}

	@Test
	void invokeAnyTest() throws InterruptedException, ExecutionException {
		ExecutorService pool = Executors.newSingleThreadExecutor();

		List<MyTask> tasks = Stream.generate(() -> new MyTask()).limit(10).collect(Collectors.toList());

		pool.invokeAny(tasks);
		assertTrue(tasks.stream().filter(t -> t.executed).count() >= 1);

	}

	@Test
	void submitTest() throws InterruptedException, ExecutionException {
		ExecutorService pool = Executors.newCachedThreadPool();

		MyTask task1 = new MyTask();
		MyTask task2 = new MyTask();

		pool.submit((Runnable) task1).get();
		assertTrue(task1.executed);

		Future<String> future = pool.submit((Callable<String>) task2);
		assertEquals("done", future.get());
		assertTrue(task2.executed);
	}

	@Test
	void finishingExecutorServiceTest() throws InterruptedException, ExecutionException {

		ExecutorService pool1 = Executors.newCachedThreadPool();
		ExecutorService pool2 = Executors.newCachedThreadPool();

		MyTask extraTask = new MyTask();

		List<MyTask> tasks1 = Stream.generate(() -> new MyTask()).limit(10).collect(Collectors.toList());
		List<MyTask> tasks2 = Stream.generate(() -> new MyTask()).limit(10).collect(Collectors.toList());

		pool1.invokeAll(tasks1);
		pool2.invokeAll(tasks2);

		pool1.shutdown();
		assertThrows(RejectedExecutionException.class, () -> pool1.submit((Callable<String>) extraTask).get());

		pool2.shutdownNow();
		assertThrows(RejectedExecutionException.class, () -> pool2.submit((Callable<String>) extraTask).get());

		assertTrue(pool1.isTerminated());
		assertTrue(pool2.isTerminated());
		assertTrue(pool1.isShutdown());
		assertTrue(pool2.isShutdown());
	}

	class MyTask implements Runnable, Callable<String> {

		boolean executed = false;

		@Override
		public void run() {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			executed = true;
		}

		@Override
		public String call() throws Exception {
			run();
			return "done";
		}

	}
}
