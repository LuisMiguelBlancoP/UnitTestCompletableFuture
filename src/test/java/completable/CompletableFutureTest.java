package completable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Uninterruptibles;

class CompletableFutureTest {

	Logger logger = Logger.getLogger("CompletableFutureTest");

	private final String FUTURE_COMPLETED = "Future Completed";
	private final String CUSTOM_COMPLETE = "Custom Completed";

	@Test
	void completableFutureCreationTest() throws InterruptedException, ExecutionException {

		// create a completed future
		CompletableFuture<String> f1 = CompletableFuture.completedFuture(FUTURE_COMPLETED);
		assertEquals(FUTURE_COMPLETED, f1.get());

		// create a future given a Runnable
		CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f2");
		});

		// create a future given a Supplier
		CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f3");
			return FUTURE_COMPLETED;
		});

		// create a future with the first future completed, in this case f3
		CompletableFuture<Object> f4 = CompletableFuture.anyOf(f2, f3);
		Object result = f4.get();
		logger.log(Level.INFO, "" + result);
		assertEquals(FUTURE_COMPLETED, result);

		// create a future with all the futures completed
		CompletableFuture<Void> f5 = CompletableFuture.allOf(f1, f2, f3);
		assertEquals(null, f5.get());

		CompletableFuture<Void> f6 = CompletableFuture.runAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f6");
		});
		CompletableFuture<String> f7 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f7");
			return FUTURE_COMPLETED;
		});

		// create a future with the first future completed, in this case f6
		CompletableFuture<Object> f8 = CompletableFuture.anyOf(f6, f7);
		Object result2 = f8.get();
		logger.log(Level.INFO, "" + result2);
		assertEquals(null, result2);
	}

	@Test
	void joinANdGetNowTest() {
		CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f1");
			return FUTURE_COMPLETED;
		});
		// use default value if the future is not completed
		assertEquals(CUSTOM_COMPLETE, f1.getNow(CUSTOM_COMPLETE));
		// wait to complete the future
		Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
		// normal value is returned when the future is complete
		assertEquals(FUTURE_COMPLETED, f1.getNow(CUSTOM_COMPLETE));

		CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f2");
			return FUTURE_COMPLETED;
		});
		assertEquals(FUTURE_COMPLETED, f2.join());
	}

	@Test
	void handleErrorsTest() throws InterruptedException, ExecutionException {
		CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			throw new NumberFormatException();
		}).handle((result, exception) -> {
			if (exception != null) {
				return CUSTOM_COMPLETE;
			}
			return result.toString();
		});

		CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			return FUTURE_COMPLETED;
		}).handle((result, exception) -> {
			if (exception != null) {
				return CUSTOM_COMPLETE;
			}
			return result.toString();
		});

		// use the value given by the handle when exception is throw
		assertEquals(CUSTOM_COMPLETE, f1.get());

		// use the value given by the initial future
		assertEquals(FUTURE_COMPLETED, f2.get());
	}

	@Test
	void finishingCompletableFutureTest() throws InterruptedException, ExecutionException {

		CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f1");
			return FUTURE_COMPLETED;
		});
		// the future is completed with given value
		f1.complete(CUSTOM_COMPLETE);
		assertEquals(CUSTOM_COMPLETE, f1.get());

		CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			return FUTURE_COMPLETED;
		});
		// the future is completed with given exception
		f2.completeExceptionally(new NumberFormatException());

		try {
			f2.get();
		} catch (ExecutionException e) {
			// the cause of the exception is the exception given by the
			// completeExceptionally function
			assertTrue(e.getCause() instanceof NumberFormatException, "The exception must be NumberFormatException");
		}

		CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f3");
			return FUTURE_COMPLETED;
		});
		// the future is cancelled if is not complete
		f3.cancel(true);

		// Verify future status
		assertTrue(f1.isDone());

		assertTrue(f2.isCompletedExceptionally());
		assertTrue(f2.isDone());

		assertTrue(f3.isCancelled());
	}

	@Test
	void forceCompletableFutureResultsTest() throws InterruptedException, ExecutionException {

		CompletableFuture<String> f1 = CompletableFuture.completedFuture(FUTURE_COMPLETED);
		assertEquals(FUTURE_COMPLETED, f1.get());

		// the future is forced to take the given value
		f1.obtrudeValue(CUSTOM_COMPLETE);
		assertEquals(CUSTOM_COMPLETE, f1.get());

		CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f2");
			return FUTURE_COMPLETED;
		});
		f2.completeExceptionally(new NumberFormatException());
		try {
			f2.get();
		} catch (ExecutionException e) {
			// the cause of the exception is the exception given by the
			// completeExceptionally function
			assertTrue(e.getCause() instanceof NumberFormatException, "The exception must be NumberFormatException");
		}

		// the future is forced to take the given exception as cause of the
		// ExecutionException
		f2.obtrudeException(new ArrayIndexOutOfBoundsException());
		try {
			f2.get();
		} catch (ExecutionException e) {
			// the cause of the exception is the exception given by the
			// completeExceptionally function
			assertTrue(e.getCause() instanceof ArrayIndexOutOfBoundsException,
					"The exception must be ArrayIndexOutOfBoundsException");
		}
	}

	@Test
	void procesingResultsTest() throws InterruptedException, ExecutionException {

		// using thenApply
		CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f1");
			return FUTURE_COMPLETED;
		}).thenApply(s -> "The result is: " + s);
		assertEquals("The result is: " + FUTURE_COMPLETED, f1.get());

		// using thenAccept
		String[] vector = { "" };
		CompletableFuture<Void> f2 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f1");
			return FUTURE_COMPLETED;
		}).thenAcceptAsync(s -> {
			vector[0] = CUSTOM_COMPLETE;
		});
		f2.get();
		assertEquals(CUSTOM_COMPLETE, vector[0]);

		// using thenRun
		String[] vector2 = { "" };
		CompletableFuture<Void> f3 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f1");
			return FUTURE_COMPLETED;
		}).thenRun(() -> {
			vector2[0] = CUSTOM_COMPLETE;
		});
		f3.get();
		assertEquals(CUSTOM_COMPLETE, vector2[0]);

	}

	@Test
	void combiningFuturesWaitingForAllCompletedTest() throws InterruptedException, ExecutionException {

		// using thenCompose
		// the result of first future is used by the second future
		CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> FUTURE_COMPLETED)
				.thenCompose(s -> CompletableFuture.supplyAsync(() -> s + CUSTOM_COMPLETE));
		assertEquals(FUTURE_COMPLETED + CUSTOM_COMPLETE, f1.get());

		// using thenCombine
		// execute both futures and use the given values to process and return a value
		CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> FUTURE_COMPLETED)
				.thenCombine(CompletableFuture.supplyAsync(() -> CUSTOM_COMPLETE), (s1, s2) -> s1 + s2);
		assertEquals(FUTURE_COMPLETED + CUSTOM_COMPLETE, f2.get());

		// using thenAcceptBoth
		// execute both futures and use the given values to execute something
		String[] vector = { "", "" };
		CompletableFuture.supplyAsync(() -> FUTURE_COMPLETED)
				.thenAcceptBoth(CompletableFuture.supplyAsync(() -> CUSTOM_COMPLETE), (s1, s2) -> {
					vector[0] = s1;
					vector[1] = s2;
				});
		assertAll(() -> assertEquals(FUTURE_COMPLETED, vector[0]), () -> assertEquals(CUSTOM_COMPLETE, vector[1]));

		// using runAfterBoth
		// execute both futures and execute something when both are completed
		String[] vector2 = { "", "" };
		CompletableFuture.supplyAsync(() -> FUTURE_COMPLETED)
				.runAfterBoth(CompletableFuture.supplyAsync(() -> CUSTOM_COMPLETE), () -> {
					vector2[0] = FUTURE_COMPLETED;
					vector2[1] = CUSTOM_COMPLETE;
				});
		assertAll(() -> assertEquals(FUTURE_COMPLETED, vector2[0]), () -> assertEquals(CUSTOM_COMPLETE, vector2[1]));
	}

	@Test
	void combiningFuturesWaitingForFirstCompletedTest() throws InterruptedException, ExecutionException {

		// using applyToEither
		// in this case f1 is completed first
		CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f1");
			return FUTURE_COMPLETED;
		});
		CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f2");
			return CUSTOM_COMPLETE;
		});

		CompletableFuture<String> f3 = f1.applyToEither(f2, s -> s);
		assertEquals(FUTURE_COMPLETED, f3.get());

		// using acceptEither
		// in this case f5 is completed first
		String[] vector = { "" };
		CompletableFuture<String> f4 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f4");
			return FUTURE_COMPLETED;
		});
		CompletableFuture<String> f5 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f5");
			return CUSTOM_COMPLETE;
		});

		CompletableFuture<Void> f6 = f4.acceptEither(f5, s -> {
			vector[0] = s;
		});
		f6.get();
		assertEquals(CUSTOM_COMPLETE, vector[0]);

		// using runAfterEither
		// in this case f8 is completed first
		String[] vector2 = { "" };
		CompletableFuture<String> f7 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f4");
			return FUTURE_COMPLETED;
		});
		CompletableFuture<String> f8 = CompletableFuture.supplyAsync(() -> {
			Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
			logger.log(Level.INFO, "Execute f5");
			return CUSTOM_COMPLETE;
		});

		CompletableFuture<Void> f9 = f7.runAfterEither(f8, () -> {
			vector2[0] = FUTURE_COMPLETED;
		});
		f9.get();
		assertEquals(FUTURE_COMPLETED, vector2[0]);
	}

	@Test
	void workingWithExecuorServiceTest() throws InterruptedException, ExecutionException {
	}
}
