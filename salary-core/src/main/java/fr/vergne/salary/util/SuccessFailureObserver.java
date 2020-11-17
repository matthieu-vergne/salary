package fr.vergne.salary.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface SuccessFailureObserver {
	void notifySuccess();

	void notifyFailure();

	default SuccessFailureObserver and(SuccessFailureObserver... others) {
		return compose(Stream//
				.concat(//
						Stream.of(this), //
						Stream.of(others))//
				.toArray(length -> new SuccessFailureObserver[length]));
	}

	public static final Runnable NO_INIT = () -> {
	};

	static SuccessFailureObserver compose(SuccessFailureObserver... others) {
		List<SuccessFailureObserver> all = Arrays.asList(others);
		Runnable onSuccess = () -> all.forEach(SuccessFailureObserver::notifySuccess);
		Runnable onFailure = () -> all.forEach(SuccessFailureObserver::notifyFailure);
		return create(NO_INIT, onSuccess, onFailure);
	}

	static SuccessFailureObserver create(Runnable init, Runnable onSuccess, Runnable onFailure) {
		init.run();
		return new SuccessFailureObserver() {

			@Override
			public void notifySuccess() {
				onSuccess.run();
			}

			@Override
			public void notifyFailure() {
				onFailure.run();
			}
		};
	}

	static SuccessFailureObserver createConsecutiveFailuresCounter(Supplier<Long> initValue, Supplier<Long> reader,
			Consumer<Long> writer) {
		Runnable init = () -> writer.accept(initValue.get());
		Runnable onSuccess = () -> writer.accept(0L);
		Runnable onFailure = () -> writer.accept(reader.get() + 1);
		return create(init, onSuccess, onFailure);
	}

	static SuccessFailureObserver createSuccessRatio(Supplier<Double> initValue, Supplier<Double> reader,
			Consumer<Double> writer, double inertia) {
		Runnable init = () -> writer.accept(initValue.get());
		Runnable onSuccess = () -> writer.accept(reader.get() * inertia + (1 - inertia));
		Runnable onFailure = () -> writer.accept(reader.get() * inertia);
		return create(init, onSuccess, onFailure);
	}
}
