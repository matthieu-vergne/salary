package fr.vergne.salary.data;

import java.util.Comparator;

import fr.vergne.salary.util.Equalable;

public interface Period {
	int start();

	int stop();

	static Period create(int value) {
		return create(value, value);
	}

	static Period create(int start, int stop) {
		return new Period() {

			@Override
			public int start() {
				return start;
			}

			@Override
			public int stop() {
				return stop;
			}

			@Override
			public String toString() {
				return start == stop ? "" + start : String.format("%d-%d", start, stop);
			}

			private final Equalable<Period> equalable = new Equalable<>(Period.class, this, //
					Period::start, //
					Period::stop);

			@Override
			public boolean equals(Object obj) {
				return equalable.equals(obj);
			}

			@Override
			public int hashCode() {
				return equalable.hashCode();
			}
		};
	}

	default boolean contains(Period that) {
		return this.start() <= that.start() //
				&& this.stop() >= that.stop();
	}

	static Comparator<Period> byStartThenStop() {
		return Comparator.comparing(Period::start).thenComparing(Period::stop);
	}
}
