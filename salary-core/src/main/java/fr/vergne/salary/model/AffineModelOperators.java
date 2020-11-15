package fr.vergne.salary.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.util.SuccessFailureObserver;

// TODO Use different parameters for each seniority
public class AffineModelOperators implements SuccessFailureObserver {
	private final Random rand;
	private final Set<Profile> profiles;

	public AffineModelOperators(Random rand, StatisticsDataset referenceStatistics) {
		this.rand = rand;
		this.profiles = referenceStatistics//
				.splitProfiles()//
				.toMap().keySet();
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

	public Supplier<Parameters> parameterGenerator() {
		return () -> {
			Affine q1 = Affine.fromSlopeIntercept(1, 1);
			Affine q3 = Affine.fromSlopeIntercept(1, 3);
			Affine mean = Affine.fromAverage(q1, q3); // FIXME Enforced by gaussian
			return Parameters.create(q1, mean, q3);
		};
	}

	interface ScaleBounds {
		int min();

		int max();

		default Integer restrict(int scale) {
			return Math.max(min(), Math.min(scale, max()));
		}

		static ScaleBounds in(int min, int max) {
			return new ScaleBounds() {

				@Override
				public int min() {
					return min;
				}

				@Override
				public int max() {
					return max;
				}

				@Override
				public String toString() {
					return String.format("[%s, %s]", min, max);
				}
			};
		}
	}

	interface Adaptation {
		Parameters apply(Parameters parameters);

		static Adaptation create(String name, Function<Parameters, Parameters> definition) {
			return new Adaptation() {

				@Override
				public Parameters apply(Parameters parameters) {
					return definition.apply(parameters);
				}

				@Override
				public String toString() {
					return name;
				}
			};
		}
	}

	// TODO Don't mix amplitude and scale
	// Scale is the dynamic adaptation parameter
	// Amplitude is the random value within the scale
	interface DynamicAdaptation extends SuccessFailureObserver {
		Adaptation adaptation(double amplitude);

		default Parameters apply(Parameters parameters) {
			return adaptation(amplitude()).apply(parameters);
		}

		int successiveFailures();

		void forgetFailures();

		double amplitude();

		double scale();

		double successRatio();

		static DynamicAdaptation create(String name, ScaleBounds scaleBounds,
				Function<Double, Function<Parameters, Parameters>> definition) {
			return new DynamicAdaptation() {

				int successiveFailures = 0;
				int scalePower = scaleBounds.restrict(Integer.MAX_VALUE);
				double successRatio = 0.5;

				@Override
				public Adaptation adaptation(double amplitude) {
					return Adaptation.create(name, definition.apply(amplitude));
				}

				@Override
				public double amplitude() {
					return scale();
				}

				@Override
				public double scale() {
					return Math.pow(2, scalePower);
				}

				@Override
				public int successiveFailures() {
					return successiveFailures;
				}

				@Override
				public void forgetFailures() {
					successiveFailures = 0;
				}

				@Override
				public double successRatio() {
					return successRatio;
				}

				@Override
				public void notifySuccess() {
					scalePower = scaleBounds.restrict(scalePower + 1);
					successRatio = successRatio * 0.95 + 0.05;
				}

				@Override
				public void notifyFailure() {
					successiveFailures++;
					scalePower = scaleBounds.restrict(scalePower - 1);
					successRatio = successRatio * 0.95;
				}

				@Override
				public String toString() {
					return name;
				}
			};
		}
	}

	private final ScaleBounds scaleBounds = ScaleBounds.in(-30, 5);
	private final List<DynamicAdaptation> adaptations = Arrays.asList(//
			// Variants

			// FIXME Forbidden by Gaussian assumption
			// params.addQ1Start(delta), //
			// params.addQ1End(delta), //
			// params.addMeanStart(delta), //
			// params.addMeanEnd(delta), //
			// params.addQ3Start(delta), //
			// params.addQ3End(delta), //

			// Move
			DynamicAdaptation.create("move start up", scaleBounds, //
					delta -> params -> params.addQ1Start(delta).addMeanStart(delta).addQ3Start(delta)),
			DynamicAdaptation.create("move start down", scaleBounds, //
					delta -> params -> params.addQ1Start(-delta).addMeanStart(-delta).addQ3Start(-delta)), //
			DynamicAdaptation.create("move end up", scaleBounds, //
					delta -> params -> params.addQ1End(delta).addMeanEnd(delta).addQ3End(delta)), //
			DynamicAdaptation.create("move end down", scaleBounds, //
					delta -> params -> params.addQ1End(-delta).addMeanEnd(-delta).addQ3End(-delta)), //
			DynamicAdaptation.create("move all up", scaleBounds, //
					delta -> params -> params.addQ1Intercept(delta).addMeanIntercept(delta).addQ3Intercept(delta)), //
			DynamicAdaptation.create("move all down", scaleBounds, //
					delta -> params -> params.addQ1Intercept(-delta).addMeanIntercept(-delta).addQ3Intercept(-delta)), //

			// Compress
			DynamicAdaptation.create("compress start", scaleBounds, //
					delta -> params -> params.addQ1Start(delta).addQ3Start(-delta)), //
			DynamicAdaptation.create("extend start", scaleBounds, //
					delta -> params -> params.addQ1Start(-delta).addQ3Start(delta)), //
			DynamicAdaptation.create("compress end", scaleBounds, //
					delta -> params -> params.addQ1End(delta).addQ3End(-delta)), //
			DynamicAdaptation.create("extend end", scaleBounds, //
					delta -> params -> params.addQ1End(-delta).addQ3End(delta)), //
			DynamicAdaptation.create("compress all", scaleBounds, //
					delta -> params -> params.addQ1Intercept(delta).addQ3Intercept(-delta)), //
			DynamicAdaptation.create("extend all", scaleBounds, //
					delta -> params -> params.addQ1Intercept(-delta).addQ3Intercept(delta)), //

			// Compress towards Q1
			DynamicAdaptation.create("extend start from Q1", scaleBounds, //
					delta -> params -> params.addMeanStart(delta / 2).addQ3Start(delta)), //
			DynamicAdaptation.create("compress start to Q1", scaleBounds, //
					delta -> params -> params.addMeanStart(-delta / 2).addQ3Start(-delta)), //
			DynamicAdaptation.create("extend end from Q1", scaleBounds, //
					delta -> params -> params.addMeanEnd(delta / 2).addQ3End(delta)), //
			DynamicAdaptation.create("compress end to Q1", scaleBounds, //
					delta -> params -> params.addMeanEnd(-delta / 2).addQ3End(-delta)), //
			DynamicAdaptation.create("extend all from Q1", scaleBounds, //
					delta -> params -> params.addMeanIntercept(delta / 2).addQ3Intercept(delta)), //
			DynamicAdaptation.create("compress all to Q1", scaleBounds, //
					delta -> params -> params.addMeanIntercept(-delta / 2).addQ3Intercept(-delta)), //

			// Compress towards Q3
			DynamicAdaptation.create("compress start to Q3", scaleBounds, //
					delta -> params -> params.addQ1Start(delta).addMeanStart(delta / 2)), //
			DynamicAdaptation.create("extend start from Q3", scaleBounds, //
					delta -> params -> params.addQ1Start(-delta).addMeanStart(-delta / 2)), //
			DynamicAdaptation.create("compress end to Q3", scaleBounds, //
					delta -> params -> params.addQ1End(delta).addMeanEnd(delta / 2)), //
			DynamicAdaptation.create("extend end from Q3", scaleBounds, //
					delta -> params -> params.addQ1End(-delta).addMeanEnd(-delta / 2)), //
			DynamicAdaptation.create("compress all to Q3", scaleBounds, //
					delta -> params -> params.addQ1Intercept(delta).addMeanIntercept(delta / 2)), //
			DynamicAdaptation.create("extend all from Q3", scaleBounds, //
					delta -> params -> params.addQ1Intercept(-delta).addMeanIntercept(-delta / 2)) //
	);

	DynamicAdaptation appliedAdaptation;

	public Function<Parameters, Parameters> parameterAdapter() {
		return parameters -> {

			Map<Parameters, DynamicAdaptation> appliedAdaptations = new HashMap<>();

			List<Parameters> candidates = adaptations.stream()//
					.map(adaptation -> {
						Parameters candidate = adaptation.apply(parameters);
						appliedAdaptations.put(candidate, adaptation);
						return candidate;
					})
					// Filter incoherent variants
					.filter(p -> p.q1().start() >= 0)//
					.filter(p -> p.q1().start() < p.q3().start())//
					.filter(p -> p.q1().end() >= 0)//
					.filter(p -> p.q1().end() < p.q3().end())//
					.collect(Collectors.toList());

			// Notify failure on invalid adaptations to adapt their scales
			Map<Parameters, DynamicAdaptation> invalidAdaptations = new HashMap<>(appliedAdaptations);
			invalidAdaptations.keySet().removeAll(candidates);
			invalidAdaptations.values().forEach(DynamicAdaptation::notifyFailure);
			invalidAdaptations.values().forEach(adaptation -> System.out.println("X " + adaptation));

			if (candidates.isEmpty()) {
				throw new IllegalStateException("No applicable adaptation");
			} else {
				Parameters newParameters = candidates.get(rand.nextInt(candidates.size()));
				appliedAdaptation = appliedAdaptations.get(newParameters);
				double appliedScale = appliedAdaptation.scale();
				System.out.println("Adaptation: " + appliedAdaptation);
				System.out.println("Scale: " + appliedScale);
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

		default Parameters addQ1Slope(double delta) {
			return create(q1().addSlope(delta), mean(), q3());
		}

		default Parameters addQ1Intercept(double delta) {
			return create(q1().addIntercept(delta), mean(), q3());
		}

		default Parameters addMeanSlope(double delta) {
			return create(q1(), mean().addSlope(delta), q3());
		}

		default Parameters addMeanIntercept(double delta) {
			return create(q1(), mean().addIntercept(delta), q3());
		}

		default Parameters addQ3Slope(double delta) {
			return create(q1(), mean(), q3().addSlope(delta));
		}

		default Parameters addQ3Intercept(double delta) {
			return create(q1(), mean(), q3().addIntercept(delta));
		}

		default Parameters addQ1Start(double delta) {
			return create(q1().addStart(delta), mean(), q3());
		}

		default Parameters addQ1End(double delta) {
			return create(q1().addEnd(delta), mean(), q3());
		}

		default Parameters addMeanStart(double delta) {
			return create(q1(), mean().addStart(delta), q3());
		}

		default Parameters addMeanEnd(double delta) {
			return create(q1(), mean().addEnd(delta), q3());
		}

		default Parameters addQ3Start(double delta) {
			return create(q1(), mean(), q3().addStart(delta));
		}

		default Parameters addQ3End(double delta) {
			return create(q1(), mean(), q3().addEnd(delta));
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

		default Affine addSlope(double delta) {
			return fromSlopeIntercept(slope() + delta, intercept());
		}

		default Affine addIntercept(double delta) {
			return fromSlopeIntercept(slope(), intercept() + delta);
		}

		default Affine movePoint(double delta, int xMove, int xFix) {
			BigDecimal xFixBD = BigDecimal.valueOf(xFix);
			BigDecimal xMoveBD = BigDecimal.valueOf(xMove);
			BigDecimal deltaBD = BigDecimal.valueOf(delta);
			BigDecimal slopeBD = BigDecimal.valueOf(slope());
			BigDecimal interceptBD = BigDecimal.valueOf(intercept());

			BigDecimal normDeltaBD = deltaBD.divide(xFixBD.subtract(xMoveBD), RoundingMode.HALF_UP);
			BigDecimal newSlopeBD = slopeBD.subtract(normDeltaBD);
			BigDecimal newInterceptBD = interceptBD.add(xFixBD.multiply(normDeltaBD));
			return fromSlopeIntercept(//
					newSlopeBD.doubleValue(), //
					newInterceptBD.doubleValue());
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
		adaptations.forEach(adaptation -> adaptation.forgetFailures());
		if (appliedAdaptation != null) {
			appliedAdaptation.notifySuccess();
		}
		displayAdaptationSummary();
	}

	@Override
	public void notifyFailure() {
		if (appliedAdaptation != null) {
			appliedAdaptation.notifyFailure();
		}
		displayAdaptationSummary();
	}

	private void displayAdaptationSummary() {
		if (appliedAdaptation != null) {
			System.out.println(String.format("Interest: %.2f%%", 100.0 / (1 + appliedAdaptation.successiveFailures())));
			System.out.println(String.format("Ratio: %.2f%%", 100 * appliedAdaptation.successRatio()));
		}
	}
}