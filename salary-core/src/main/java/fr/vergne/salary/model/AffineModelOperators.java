package fr.vergne.salary.model;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.StatisticsDataset;

// TODO Use different parameters for each seniority
public class AffineModelOperators {
	private final Random rand;
	private final StatisticsDataset referenceStatistics;

	public AffineModelOperators(Random rand, StatisticsDataset referenceStatistics) {
		this.rand=rand;
		this.referenceStatistics=referenceStatistics;
	}
	
	public Supplier<StatsAffine> parameterGenerator() {
		return () -> {
			Affine q1 = Affine.fromSlopeIntercept(1, 1);
			Affine q3 = Affine.fromSlopeIntercept(1, 3);
			Affine mean = Affine.fromAverage(q1, q3); // FIXME Enforced by gaussian
			return StatsAffine.create(q1, mean, q3);
		};
	}

	public Function<StatsAffine, StatsAffine> parameterAdapter() {
		return params -> {
			double delta = rand.nextDouble() - 0.5;
			List<StatsAffine> candidates = Stream.of(//
					// Variants

					// FIXME Forbidden by Gaussian assumption
					// params.addQ1Start(delta), //
					// params.addQ1End(delta), //
					// params.addMeanStart(delta), //
					// params.addMeanEnd(delta), //
					// params.addQ3Start(delta), //
					// params.addQ3End(delta), //

					// Move
					params.addQ1Start(delta).addMeanStart(delta).addQ3Start(delta), //
					params.addQ1End(delta).addMeanEnd(delta).addQ3End(delta), //
					params.addQ1Intercept(delta).addMeanIntercept(delta).addQ3Intercept(delta), //

					// Compress
					params.addQ1Start(delta).addQ3Start(-delta), //
					params.addQ1End(delta).addQ3End(-delta), //
					params.addQ1Intercept(delta).addQ3Intercept(-delta), //

					// Compress towards Q1
					params.addMeanStart(delta / 2).addQ3Start(delta), //
					params.addMeanEnd(delta / 2).addQ3End(delta), //
					params.addMeanIntercept(delta / 2).addQ3Intercept(delta), //

					// Compress towards Q3
					params.addQ1Start(delta).addMeanStart(delta / 2), //
					params.addQ1End(delta).addMeanEnd(delta / 2), //
					params.addQ1Intercept(delta).addMeanIntercept(delta / 2) //
			)//
				// Filter incoherent variants
					.filter(p -> p.q1().start() >= 0)//
					.filter(p -> p.q1().start() < p.q3().start())//
					.filter(p -> p.q1().end() >= 0)//
					.filter(p -> p.q1().end() < p.q3().end())//
					.collect(Collectors.toList());
			return candidates.isEmpty() ? params : //
			candidates.get(rand.nextInt(candidates.size()));
		};
	}

	public Function<StatsAffine, Model<StatsAffine>> modelFactory() {
		return params -> {
			String name = "" + params;
			StatisticsDataset dataset = StatisticsDataset.fromMap(referenceStatistics//
					.splitProfiles()//
					.toMap().keySet().stream()//
					.collect(Collectors.toMap(//
							profile -> profile, //
							profile -> {
								int exp = profile.experience().start();
								return Statistics.create(//
										params.q1().resolve(exp), //
										params.mean().resolve(exp), //
										params.q3().resolve(exp));
							})));
			return Model.create(params, name, dataset);
		};
	}

	interface Affine {
		final int expStart = 1;// TODO min experience
		final int expEnd = 14;// TODO max experience

		double slope();

		double intercept();

		default double start() {
			return resolve(expStart);
		}

		default double end() {
			return resolve(expEnd);
		}

		default double resolve(double x) {
			return slope() * x + intercept();
		}

		default Affine addSlope(double delta) {
			return fromSlopeIntercept(slope() + delta, intercept());
		}

		default Affine addIntercept(double delta) {
			return fromSlopeIntercept(slope(), intercept() + delta);
		}

		default Affine movePoint(double delta, int xMove, int xFix) {
			double normDelta = delta / (xFix - xMove);
			return fromSlopeIntercept(//
					this.slope() - normDelta, //
					this.intercept() + xFix * normDelta);
		}

		default Affine addStart(double delta) {
			return this.movePoint(delta, expStart, expEnd);
		}

		default Affine addEnd(double delta) {
			return this.movePoint(delta, expEnd, expStart);
		}

		static Affine fromSlopeIntercept(double slope, double intercept) {
			return new Affine() {
				@Override
				public double slope() {
					return slope;
				}

				@Override
				public double intercept() {
					return intercept;
				}

				@Override
				public String toString() {
					return String.format("[%.1f;%.1f]", start(), end());
				}
			};
		}

		static Affine fromAverage(Affine... affines) {
			return fromSlopeIntercept(//
					Stream.of(affines).mapToDouble(Affine::slope).average().getAsDouble(), //
					Stream.of(affines).mapToDouble(Affine::intercept).average().getAsDouble());
		}

	}

	interface StatsAffine {

		Affine q1();

		Affine mean();

		Affine q3();

		static StatsAffine create(Affine q1, Affine mean, Affine q3) {
			return new StatsAffine() {

				@Override
				public Affine q1() {
					return q1;
				}

				@Override
				public Affine mean() {
					return mean;
				}

				@Override
				public Affine q3() {
					return q3;
				}

				@Override
				public String toString() {
					return String.format("%s < %s < %s", q1, mean, q3);
				}
			};
		}

		default StatsAffine addQ1Slope(double delta) {
			return create(q1().addSlope(delta), mean(), q3());
		}

		default StatsAffine addQ1Intercept(double delta) {
			return create(q1().addIntercept(delta), mean(), q3());
		}

		default StatsAffine addMeanSlope(double delta) {
			return create(q1(), mean().addSlope(delta), q3());
		}

		default StatsAffine addMeanIntercept(double delta) {
			return create(q1(), mean().addIntercept(delta), q3());
		}

		default StatsAffine addQ3Slope(double delta) {
			return create(q1(), mean(), q3().addSlope(delta));
		}

		default StatsAffine addQ3Intercept(double delta) {
			return create(q1(), mean(), q3().addIntercept(delta));
		}

		default StatsAffine addQ1Start(double delta) {
			return create(q1().addStart(delta), mean(), q3());
		}

		default StatsAffine addQ1End(double delta) {
			return create(q1().addEnd(delta), mean(), q3());
		}

		default StatsAffine addMeanStart(double delta) {
			return create(q1(), mean().addStart(delta), q3());
		}

		default StatsAffine addMeanEnd(double delta) {
			return create(q1(), mean().addEnd(delta), q3());
		}

		default StatsAffine addQ3Start(double delta) {
			return create(q1(), mean(), q3().addStart(delta));
		}

		default StatsAffine addQ3End(double delta) {
			return create(q1(), mean(), q3().addEnd(delta));
		}
	}
}
