package fr.vergne.salary.data;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Statistics {

	public static final Statistics ZEROS = create(0, 0, 0);

	double Q1();

	double mean();

	double Q3();

	public static enum Type {
		Q1(Statistics::Q1), MEAN(Statistics::mean), Q3(Statistics::Q3);

		private final Function<Statistics, Double> method;

		private Type(Function<Statistics, Double> method) {
			this.method = method;
		}

		public double from(Statistics reference) {
			return method.apply(reference);
		}
	};

	static Statistics create(//
			Supplier<Double> Q1, //
			Supplier<Double> mean, //
			Supplier<Double> Q3) {
		return new Statistics() {

			@Override
			public double Q1() {
				return Q1.get();
			}

			@Override
			public double mean() {
				return mean.get();
			}

			@Override
			public double Q3() {
				return Q3.get();
			}
		};
	}

	default Statistics transform(//
			Function<Statistics, Double> Q1, //
			Function<Statistics, Double> mean, //
			Function<Statistics, Double> Q3) {
		return create(//
				() -> Q1.apply(this), //
				() -> mean.apply(this), //
				() -> Q3.apply(this));
	}

	static Statistics create(double Q1, double mean, double Q3) {
		return create(() -> Q1, () -> mean, () -> Q3);
	}

	default Statistics snapshot() {
		return create(Q1(), mean(), Q3());
	}

	interface Factor {
		double get(Type type);
	}

	default Statistics factor(Factor factor) {
		return transform(//
				stat -> factor.get(Type.Q1) * stat.Q1(), //
				stat -> factor.get(Type.MEAN) * stat.mean(), //
				stat -> factor.get(Type.Q3) * stat.Q3());
	}
}
