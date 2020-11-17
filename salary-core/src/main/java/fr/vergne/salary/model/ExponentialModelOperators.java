package fr.vergne.salary.model;

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

// TODO Use different parameters for each seniority
public class ExponentialModelOperators {
	private final Random rand;
	private final Set<Profile> profiles;
	private int randPower = 0;

	public ExponentialModelOperators(Random rand, Set<Profile> profiles) {
		this.rand = rand;
		this.profiles = profiles;
	}

	public Supplier<Parameters> parameterGenerator() {
		return () -> {
			Exponential q1 = Exponential.fromScaleBaseIntercept(1, 1, 1);
			Exponential q3 = Exponential.fromScaleBaseIntercept(1, 1, 3);
			Exponential mean = Exponential.fromAverage(q1, q3); // FIXME Enforced by gaussian
			return Parameters.create(q1, mean, q3);
		};
	}

	public Function<Parameters, Parameters> parameterAdapter() {
		return params -> {
			double randScale = 10;
			System.out.println("Rand scale: " + randScale);
			double delta = randScale * (rand.nextDouble() - 0.5);

			List<Parameters> candidates = Stream.of(//
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
					params.addQ1Intercept(delta).addMeanIntercept(delta / 2), //

					// Redress
					params.addQ1Base(delta / 100).addMeanBase(delta / 100).addQ3Base(delta / 100) //
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

	interface Parameters {

		Exponential q1();

		Exponential mean();

		Exponential q3();

		static Parameters create(Exponential q1, Exponential mean, Exponential q3) {
			return new Parameters() {

				@Override
				public Exponential q1() {
					return q1;
				}

				@Override
				public Exponential mean() {
					return mean;
				}

				@Override
				public Exponential q3() {
					return q3;
				}

				@Override
				public String toString() {
					return String.format("%s < %s < %s", q1, mean, q3);
				}
			};
		}

		default Parameters addQ1Scale(double delta) {
			return create(q1().addScale(delta), mean(), q3());
		}

		default Parameters addQ1Base(double delta) {
			return create(q1().addBase(delta), mean(), q3());
		}

		default Parameters addQ1Intercept(double delta) {
			return create(q1().addIntercept(delta), mean(), q3());
		}

		default Parameters addMeanScale(double delta) {
			return create(q1(), mean().addScale(delta), q3());
		}

		default Parameters addMeanBase(double delta) {
			return create(q1(), mean().addBase(delta), q3());
		}

		default Parameters addMeanIntercept(double delta) {
			return create(q1(), mean().addIntercept(delta), q3());
		}

		default Parameters addQ3Scale(double delta) {
			return create(q1(), mean(), q3().addScale(delta));
		}

		default Parameters addQ3Base(double delta) {
			return create(q1(), mean(), q3().addBase(delta));
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

	interface Exponential {
		final int expStart = 1;// TODO min experience
		final int expEnd = 14;// TODO max experience

		double scale();

		double base();

		double intercept();

		default double start() {
			return resolve(expStart);
		}

		default double end() {
			return resolve(expEnd);
		}

		default double resolve(double x) {
			return scale() * Math.pow(base(), x) + intercept();
		}

		default Exponential addScale(double delta) {
			return fromScaleBaseIntercept(scale() + delta, base(), intercept());
		}

		default Exponential addBase(double delta) {
			return fromScaleBaseIntercept(scale(), base() + delta, intercept());
		}

		default Exponential addIntercept(double delta) {
			return fromScaleBaseIntercept(scale(), base(), intercept() + delta);
		}

		default Exponential movePoint(double delta, int xMove, int xFix) {
			double base = this.base();
			double bFix = Math.pow(base, xFix);
			double bMove = Math.pow(base, xMove);
			double normDelta = delta / (bFix - bMove);
			double scale = this.scale() - normDelta;
			double intercept = this.intercept() + bFix * normDelta;
			return fromScaleBaseIntercept(//
					scale, //
					base, intercept);
		}

		default Exponential addStart(double delta) {
			return this.movePoint(delta, expStart, expEnd);
		}

		default Exponential addEnd(double delta) {
			return this.movePoint(delta, expEnd, expStart);
		}

		static Exponential fromScaleBaseIntercept(double scale, double base, double intercept) {
			return new Exponential() {
				@Override
				public double scale() {
					return scale;
				}

				@Override
				public double base() {
					return base;
				}

				@Override
				public double intercept() {
					return intercept;
				}

				@Override
				public String toString() {
					return String.format("[%.1f;%.1f]=%.1f", start(), end(), end() - start());
				}
			};
		}

		static Exponential fromAverage(Exponential... affines) {
			return fromScaleBaseIntercept(//
					Stream.of(affines).mapToDouble(Exponential::scale).average().getAsDouble(), //
					// TODO check base
					Stream.of(affines).mapToDouble(Exponential::base).average().getAsDouble(), //
					Stream.of(affines).mapToDouble(Exponential::intercept).average().getAsDouble());
		}

	}

	public void notifySuccess() {
		randPower++;
	}

	public void notifyFailure() {
		randPower--;
	}
}
