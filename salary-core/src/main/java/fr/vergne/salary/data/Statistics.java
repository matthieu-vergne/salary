package fr.vergne.salary.data;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Statistics {

	public static final Statistics ZEROS = create(0, 0, 0);

	double q1();

	double mean();

	double q3();

	public static enum Type {
		Q1(Statistics::q1), MEAN(Statistics::mean), Q3(Statistics::q3);

		private final Function<Statistics, Double> method;

		private Type(Function<Statistics, Double> method) {
			this.method = method;
		}

		public double from(Statistics reference) {
			return method.apply(reference);
		}
	};

	static Statistics create(//
			Supplier<Double> q1, //
			Supplier<Double> mean, //
			Supplier<Double> q3) {
		return new Statistics() {

			@Override
			public double q1() {
				return q1.get();
			}

			@Override
			public double mean() {
				return mean.get();
			}

			@Override
			public double q3() {
				return q3.get();
			}
		};
	}

	default Statistics transform(//
			Function<Statistics, Double> q1, //
			Function<Statistics, Double> mean, //
			Function<Statistics, Double> q3) {
		return create(//
				() -> q1.apply(this), //
				() -> mean.apply(this), //
				() -> q3.apply(this));
	}

	static Statistics create(double q1, double mean, double q3) {
		return create(() -> q1, () -> mean, () -> q3);
	}

	default Statistics snapshot() {
		return create(q1(), mean(), q3());
	}

	interface Factor {
		double get(Type type);
	}

	default Statistics factor(Factor factor) {
		return transform(//
				stat -> factor.get(Type.Q1) * stat.q1(), //
				stat -> factor.get(Type.MEAN) * stat.mean(), //
				stat -> factor.get(Type.Q3) * stat.q3());
	}
}
