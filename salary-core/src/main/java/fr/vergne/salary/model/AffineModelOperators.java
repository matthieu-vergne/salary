package fr.vergne.salary.model;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.util.Bounds;
import fr.vergne.salary.util.SuccessFailureObserver;

// TODO Use different parameters for each seniority
public class AffineModelOperators implements SuccessFailureObserver {
	private final Random rand;
	private final Set<Profile> profiles;
	private final PrintStream printStream;

	public AffineModelOperators(Random rand, Set<Profile> profiles, PrintStream printStream) {
		this.rand = rand;
		this.profiles = profiles;
		this.printStream = printStream;
	}

	public Function<Parameters, String> parametersFormatter() {
		return parameter -> {
			Affine q1 = parameter.q1();
			Affine mean = parameter.mean();
			Affine q3 = parameter.q3();
			return String.format("%.1f<Q1<%.1f | %.1f<mean<%.1f | %.1f<Q3<%.1f", //
					q1.start(), q1.end(), mean.start(), mean.end(), q3.start(), q3.end());
		};
	}

	public Supplier<Parameters> parametersGenerator() {
		return () -> {
			Affine q1 = Affine.fromSlopeIntercept(1, 1);
			Affine q3 = Affine.fromSlopeIntercept(1, 3);
			Affine mean = Affine.fromAverage(q1, q3); // FIXME Enforced by gaussian
			return Parameters.create(q1, mean, q3);
		};
	}

	static class ParametersMutator implements Mutator<Parameters>, SuccessFailureObserver {

		private long valuePower;
		private long consecutiveFailures;
		private double successRatio;
		private final SuccessFailureObserver updater;

		private final Mutator<Parameters> mutator;

		private ParametersMutator(String name, Bounds<Long> mutationScaleBounds,
				BiFunction<Parameters, Double, Parameters> definition) {
			this.updater = SuccessFailureObserver.compose(//
					SuccessFailureObserver.createConsecutiveFailuresCounter(//
							() -> 0L, //
							() -> consecutiveFailures, //
							value -> consecutiveFailures = value), //
					SuccessFailureObserver.createSuccessRatio(//
							() -> 0.5, //
							() -> successRatio, //
							value -> successRatio = value, //
							0.9), //
					SuccessFailureObserver.create(//
							() -> valuePower = mutationScaleBounds.max(), //
							() -> valuePower = mutationScaleBounds.restrict(valuePower + 1), //
							() -> valuePower = mutationScaleBounds.restrict(valuePower - 1)));

			this.mutator = Mutator.create(//
					() -> String.format(name + " 2^" + valuePower), //
					params -> definition.apply(params, Math.pow(2, valuePower)));
		}

		@Override
		public Parameters apply(Parameters parameters) {
			return mutator.apply(parameters);
		}

		@Override
		public String toString() {
			return mutator.toString();
		}

		@Override
		public void notifySuccess() {
			updater.notifySuccess();
		}

		@Override
		public void notifyFailure() {
			updater.notifyFailure();
		}

		public void forgetFailures() {
			consecutiveFailures = 0;
		}

		static ParametersMutator create(String name, Bounds<Long> mutationScaleBounds,
				BiFunction<Parameters, Double, Parameters> definition) {
			return new ParametersMutator(name, mutationScaleBounds, definition);
		}
	}

	private final Bounds<Long> mutationScaleBounds = Bounds.in(-6L, 5L);
	private final List<ParametersMutator> mutators = Arrays.asList(//
			// FIXME Forbidden by Gaussian assumption
			// params.addQ1Start(value), //
			// params.addQ1End(value), //
			// params.addMeanStart(value), //
			// params.addMeanEnd(value), //
			// params.addQ3Start(value), //
			// params.addQ3End(value), //

			// Move
			ParametersMutator.create("move start up", mutationScaleBounds, //
					(params, value) -> params.addQ1Start(value).addMeanStart(value).addQ3Start(value)),
			ParametersMutator.create("move start down", mutationScaleBounds, //
					(params, value) -> params.addQ1Start(-value).addMeanStart(-value).addQ3Start(-value)), //
			ParametersMutator.create("move end up", mutationScaleBounds, //
					(params, value) -> params.addQ1End(value).addMeanEnd(value).addQ3End(value)), //
			ParametersMutator.create("move end down", mutationScaleBounds, //
					(params, value) -> params.addQ1End(-value).addMeanEnd(-value).addQ3End(-value)), //
			ParametersMutator.create("move all up", mutationScaleBounds, //
					(params, value) -> params.addQ1Intercept(value).addMeanIntercept(value).addQ3Intercept(value)), //
			ParametersMutator.create("move all down", mutationScaleBounds, //
					(params, value) -> params.addQ1Intercept(-value).addMeanIntercept(-value).addQ3Intercept(-value)), //

			// Compress
			ParametersMutator.create("compress start", mutationScaleBounds, //
					(params, value) -> params.addQ1Start(value).addQ3Start(-value)), //
			ParametersMutator.create("extend start", mutationScaleBounds, //
					(params, value) -> params.addQ1Start(-value).addQ3Start(value)), //
			ParametersMutator.create("compress end", mutationScaleBounds, //
					(params, value) -> params.addQ1End(value).addQ3End(-value)), //
			ParametersMutator.create("extend end", mutationScaleBounds, //
					(params, value) -> params.addQ1End(-value).addQ3End(value)), //
			ParametersMutator.create("compress all", mutationScaleBounds, //
					(params, value) -> params.addQ1Intercept(value).addQ3Intercept(-value)), //
			ParametersMutator.create("extend all", mutationScaleBounds, //
					(params, value) -> params.addQ1Intercept(-value).addQ3Intercept(value)), //

			// Compress towards Q1
			ParametersMutator.create("extend start from Q1", mutationScaleBounds, //
					(params, value) -> params.addMeanStart(value / 2).addQ3Start(value)), //
			ParametersMutator.create("compress start to Q1", mutationScaleBounds, //
					(params, value) -> params.addMeanStart(-value / 2).addQ3Start(-value)), //
			ParametersMutator.create("extend end from Q1", mutationScaleBounds, //
					(params, value) -> params.addMeanEnd(value / 2).addQ3End(value)), //
			ParametersMutator.create("compress end to Q1", mutationScaleBounds, //
					(params, value) -> params.addMeanEnd(-value / 2).addQ3End(-value)), //
			ParametersMutator.create("extend all from Q1", mutationScaleBounds, //
					(params, value) -> params.addMeanIntercept(value / 2).addQ3Intercept(value)), //
			ParametersMutator.create("compress all to Q1", mutationScaleBounds, //
					(params, value) -> params.addMeanIntercept(-value / 2).addQ3Intercept(-value)), //

			// Compress towards Q3
			ParametersMutator.create("compress start to Q3", mutationScaleBounds, //
					(params, value) -> params.addQ1Start(value).addMeanStart(value / 2)), //
			ParametersMutator.create("extend start from Q3", mutationScaleBounds, //
					(params, value) -> params.addQ1Start(-value).addMeanStart(-value / 2)), //
			ParametersMutator.create("compress end to Q3", mutationScaleBounds, //
					(params, value) -> params.addQ1End(value).addMeanEnd(value / 2)), //
			ParametersMutator.create("extend end from Q3", mutationScaleBounds, //
					(params, value) -> params.addQ1End(-value).addMeanEnd(-value / 2)), //
			ParametersMutator.create("compress all to Q3", mutationScaleBounds, //
					(params, value) -> params.addQ1Intercept(value).addMeanIntercept(value / 2)), //
			ParametersMutator.create("extend all from Q3", mutationScaleBounds, //
					(params, value) -> params.addQ1Intercept(-value).addMeanIntercept(-value / 2)) //
	);

	ParametersMutator appliedMutator;

	public Function<Parameters, Parameters> parametersAdapter() {
		return parameters -> {

			Map<Parameters, ParametersMutator> appliedMutators = new HashMap<>();

			List<Parameters> candidates = mutators.stream()//
					.map(mutator -> {
						Parameters candidate = mutator.apply(parameters);
						appliedMutators.put(candidate, mutator);
						return candidate;
					})
					// Filter incoherent mutations
					.filter(p -> p.q1().start() >= 0)//
					.filter(p -> p.q1().start() < p.q3().start())//
					.filter(p -> p.q1().end() >= 0)//
					.filter(p -> p.q1().end() < p.q3().end())//
					.collect(Collectors.toList());

			// Notify failure on invalid mutations to adapt their values
			Map<Parameters, ParametersMutator> invalidMutators = new HashMap<>(appliedMutators);
			invalidMutators.keySet().removeAll(candidates);
			invalidMutators.values().forEach(mutator -> {
				printStream.println("X " + mutator);
				mutator.notifyFailure();
			});

			if (candidates.isEmpty()) {
				throw new IllegalStateException("No applicable adaptation");
			} else {
				Parameters newParameters = candidates.get(rand.nextInt(candidates.size()));
				appliedMutator = appliedMutators.get(newParameters);
				printStream.println("Adaptation: " + appliedMutator);
				return newParameters;
			}
		};
	}

	public Function<Parameters, Model<Parameters>> modelFactory() {
		return params -> {
			String name = "" + params;
			Map<Profile, Statistics> stats = profiles.stream().collect(Collectors.toMap(//
					profile -> profile, //
					profile -> {
						int exp = profile.experience().start();
						return Statistics.create(//
								params.q1().resolve(exp), //
								params.mean().resolve(exp), //
								params.q3().resolve(exp));
					}));
			StatisticsDataset dataset = StatisticsDataset.fromMap(stats);
			return Model.create(params, name, dataset);
		};
	}

	public interface Parameters {

		Affine q1();

		Affine mean();

		Affine q3();

		static Parameters create(Affine q1, Affine mean, Affine q3) {
			return new Parameters() {

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
					return Map.of("q1", q1, "mean", mean, "q3", q3).toString();
				}
			};
		}

		default Parameters addQ1Slope(double value) {
			return create(q1().addSlope(value), mean(), q3());
		}

		default Parameters addQ1Intercept(double value) {
			return create(q1().addIntercept(value), mean(), q3());
		}

		default Parameters addMeanSlope(double value) {
			return create(q1(), mean().addSlope(value), q3());
		}

		default Parameters addMeanIntercept(double value) {
			return create(q1(), mean().addIntercept(value), q3());
		}

		default Parameters addQ3Slope(double value) {
			return create(q1(), mean(), q3().addSlope(value));
		}

		default Parameters addQ3Intercept(double value) {
			return create(q1(), mean(), q3().addIntercept(value));
		}

		default Parameters addQ1Start(double value) {
			return create(q1().addStart(value), mean(), q3());
		}

		default Parameters addQ1End(double value) {
			return create(q1().addEnd(value), mean(), q3());
		}

		default Parameters addMeanStart(double value) {
			return create(q1(), mean().addStart(value), q3());
		}

		default Parameters addMeanEnd(double value) {
			return create(q1(), mean().addEnd(value), q3());
		}

		default Parameters addQ3Start(double value) {
			return create(q1(), mean(), q3().addStart(value));
		}

		default Parameters addQ3End(double value) {
			return create(q1(), mean(), q3().addEnd(value));
		}
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
			BigDecimal slopeBD = BigDecimal.valueOf(slope());
			BigDecimal xBD = BigDecimal.valueOf(x);
			BigDecimal interceptBD = BigDecimal.valueOf(intercept());

			BigDecimal yBD = slopeBD.multiply(xBD).add(interceptBD);
			return yBD.doubleValue();
		}

		default Affine addSlope(double value) {
			return fromSlopeIntercept(slope() + value, intercept());
		}

		default Affine addIntercept(double value) {
			return fromSlopeIntercept(slope(), intercept() + value);
		}

		default Affine movePoint(double value, int xMove, int xFix) {
			BigDecimal xFixBD = BigDecimal.valueOf(xFix);
			BigDecimal xMoveBD = BigDecimal.valueOf(xMove);
			BigDecimal valueBD = BigDecimal.valueOf(value);
			BigDecimal slopeBD = BigDecimal.valueOf(slope());
			BigDecimal interceptBD = BigDecimal.valueOf(intercept());

			BigDecimal normValueBD = valueBD.divide(xFixBD.subtract(xMoveBD), RoundingMode.HALF_UP);
			BigDecimal newSlopeBD = slopeBD.subtract(normValueBD);
			BigDecimal newInterceptBD = interceptBD.add(xFixBD.multiply(normValueBD));
			return fromSlopeIntercept(//
					newSlopeBD.doubleValue(), //
					newInterceptBD.doubleValue());
		}

		default Affine addStart(double value) {
			return this.movePoint(value, expStart, expEnd);
		}

		default Affine addEnd(double value) {
			return this.movePoint(value, expEnd, expStart);
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
					return String.format("%s*exp+%s", slope, intercept);
				}
			};
		}

		static Affine fromAverage(Affine... affines) {
			return fromSlopeIntercept(//
					Stream.of(affines).mapToDouble(Affine::slope).average().getAsDouble(), //
					Stream.of(affines).mapToDouble(Affine::intercept).average().getAsDouble());
		}

	}

	@Override
	public void notifySuccess() {
		mutators.forEach(ParametersMutator::forgetFailures);
		if (appliedMutator != null) {
			appliedMutator.notifySuccess();
		}
		displayAdaptationSummary();
	}

	@Override
	public void notifyFailure() {
		if (appliedMutator != null) {
			appliedMutator.notifyFailure();
		}
		displayAdaptationSummary();
	}

	private void displayAdaptationSummary() {
		if (appliedMutator != null) {
			printStream.println(String.format("Interest: %.2f%%", 100.0 / (1 + appliedMutator.consecutiveFailures)));
			printStream.println(String.format("Ratio: %.2f%%", 100 * appliedMutator.successRatio));
		}
	}
}