package fr.vergne.salary.util;

public interface Bounds<T extends Comparable<T>> {
	T min();

	T max();

	default T restrict(T value) {
		T min = min();
		T max = max();
		return value.compareTo(min) < 0 ? min : //
				value.compareTo(max) > 0 ? max : //
						value;
	}

	static <T extends Comparable<T>> Bounds<T> in(T min, T max) {
		return new Bounds<>() {

			@Override
			public T min() {
				return min;
			}

			@Override
			public T max() {
				return max;
			}

			@Override
			public String toString() {
				return String.format("[%s, %s]", min, max);
			}
		};
	}
}
