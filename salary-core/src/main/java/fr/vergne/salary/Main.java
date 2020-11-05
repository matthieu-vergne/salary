package fr.vergne.salary;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import fr.vergne.salary.Main.Statistics.Type;

public class Main {

	public static void main(String[] args) {
		System.out.println("Create reference data");
		Map<Profile, Statistics> referenceData = createReferenceData();

		System.out.println("Create model");
		ModelGenerator modelGenerator = new ModelGenerator(new RandomGenerator(0));
		Map<Profile, Statistics> splitData = splitProfiles(referenceData);
		double[] statisticsFactor = { 1 };
		Map<Profile, Statistics> modelData = createFactoredData(splitData, (profile, statType) -> statisticsFactor[0]);
		Model model = modelGenerator.createDataBasedModel(modelData);

		// TODO Downhill algorithm to converge parameter to 1
		// TODO Increase to 1 parameter per type
		// TODO Increase to 1 parameter per profile & type
		// TODO GA to converge parameters to reference
		// TODO Parametered model on linear curves
		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves

		System.out.println("Create salaries from model");
		int salariesPerProfile = 100000;
		Set<Profile> modelProfiles = modelData.keySet();
		Map<Profile, Collection<Double>> salaries = generateSalaries(model, modelProfiles, salariesPerProfile);
		SalariesStatistics samplesStatistics = SalariesStatistics.fromSalariesMap(salaries);

		System.out.println("Compute model error from model data:");
		Map<Type, double[]> errorBoundsMap = new LinkedHashMap<>();
		errorBoundsMap.put(Type.Q1, new double[] { 1, 0 });
		errorBoundsMap.put(Type.MEAN, new double[] { 1, 0 });
		errorBoundsMap.put(Type.Q3, new double[] { 1, 0 });
		DiffConsumer updateErrorBounds = (profile, type, actual, target) -> {
			double[] errorBounds = errorBoundsMap.get(type);
			double error = computeError(actual, target);
			errorBounds[0] = Math.min(errorBounds[0], error);
			errorBounds[1] = Math.max(errorBounds[1], error);
		};
		compareToTarget(samplesStatistics, modelData, updateErrorBounds);
		displayGlobalErrorBounds(errorBoundsMap);

		System.out.println("Compute model error from reference data:");
		errorBoundsMap.put(Type.Q1, new double[] { 1, 0 });
		errorBoundsMap.put(Type.MEAN, new double[] { 1, 0 });
		errorBoundsMap.put(Type.Q3, new double[] { 1, 0 });
		@SuppressWarnings("unused")
		DiffConsumer displayDetails = (profile, type, actual, target) -> {
			if (type.equals(Type.Q1)) {
				System.out.println(profile);
			}
			double error = computeError(actual, target);
			System.out.println(String.format("  Δ%s=|%.2f-%.2f|=%.2f%%", type, target, actual, error));
		};
		compareToTarget(samplesStatistics, referenceData, (profile, type, actual, target) -> {
			updateErrorBounds.consume(profile, type, actual, target);
			// displayDetails.consume(profile, type, actual, target);
		});
		displayGlobalErrorBounds(errorBoundsMap);
		
		System.out.println("Done");
	}

	private static void displayGlobalErrorBounds(Map<Type, double[]> errorBoundsMap) {
		errorBoundsMap.entrySet().forEach(entry -> {
			Type type = entry.getKey();
			double[] errorBounds = entry.getValue();
			System.out.println(String.format("  Δ%s ∈ [%.2f%% ; %.2f%%]", type, errorBounds[0], errorBounds[1]));
		});
	}

	private static double computeError(double actual, double target) {
		return 100 * Math.abs(target - actual) / target;
	}

	private static void compareToTarget(SalariesStatistics generatedStatistics, Map<Profile, Statistics> targetData,
			DiffConsumer consumer) {
		for (Profile profile : targetData.keySet().stream().sorted(bySeniorityThenExperience()).collect(toList())) {
			Statistics actualStats = generatedStatistics.filterOnProfile(profile);
			Statistics targetStats = targetData.get(profile);
			consumer.consume(profile, Type.Q1, actualStats, targetStats);
			consumer.consume(profile, Type.MEAN, actualStats, targetStats);
			consumer.consume(profile, Type.Q3, actualStats, targetStats);
		}
	}

	interface DiffConsumer {
		void consume(Profile profile, Type type, double actual, double target);

		default void consume(Profile profile, Type type, Statistics actualStats, Statistics targetStats) {
			consume(profile, type, type.from(actualStats), type.from(targetStats));
		}
	}

	private static Map<Profile, Statistics> splitProfiles(Map<Profile, Statistics> data) {
		return data.entrySet().stream()//
				// Split each aggregated profile into unit profiles with same statistics
				.flatMap(entry -> {
					Profile aggregatedProfile = entry.getKey();
					Statistics statistics = entry.getValue();
					return Stream.of(aggregatedProfile)//
							// Split experience in units
							.flatMap(profile -> IntStream
									.rangeClosed(profile.experience().start(), profile.experience().stop())//
									.mapToObj(year -> Period.createInstant(year))//
									.map(instant -> Profile.create(profile.seniority(), instant)))//
							// Split seniority in units
							.flatMap(profile -> IntStream
									.rangeClosed(profile.seniority().start(), profile.seniority().stop())//
									.mapToObj(year -> Period.createInstant(year))//
									.map(instant -> Profile.create(instant, profile.experience())))//
							// Pair with statistics
							.map(unitProfile -> Map.entry(unitProfile, statistics));
				})// Collect all
				.collect(toMap(//
						Entry<Profile, Statistics>::getKey, //
						Entry<Profile, Statistics>::getValue));
	}

	private static Comparator<Profile> bySeniorityThenExperience() {
		Comparator<Period> startComparator = Comparator.comparing(Period::start);
		Comparator<Period> stopComparator = Comparator.comparing(Period::stop);
		Comparator<Period> periodComparator = startComparator.thenComparing(stopComparator);

		Comparator<Profile> seniorityComparator = Comparator.comparing(Profile::seniority, periodComparator);
		Comparator<Profile> experienceComparator = Comparator.comparing(Profile::experience, periodComparator);
		Comparator<Profile> profileComparator = seniorityComparator.thenComparing(experienceComparator);

		return profileComparator;
	}

	private static Map<Profile, Statistics> createReferenceData() {
		Period sen_0_1 = Period.create(0, 1);
		Period sen_2_5 = Period.create(2, 5);

		Period exp_1____ = Period.createInstant(1);
		Period exp_2_5__ = Period.create(2, 5);
		Period exp_6_9__ = Period.create(6, 9);
		Period exp_10_14 = Period.create(10, 14);

		return Map.of(//
				Profile.create(sen_0_1, exp_1____), Statistics.create(29.731, 34.062, 38.942), //
				Profile.create(sen_0_1, exp_2_5__), Statistics.create(31.465, 35.797, 40.640), //
				Profile.create(sen_0_1, exp_6_9__), Statistics.create(35.429, 40.476, 46.144), //
				Profile.create(sen_0_1, exp_10_14), Statistics.create(39.964, 45.670, 52.082), //

				Profile.create(sen_2_5, exp_1____), Statistics.create(29.837, 34.125, 38.948), //
				Profile.create(sen_2_5, exp_2_5__), Statistics.create(31.577, 35.863, 40.646), //
				Profile.create(sen_2_5, exp_6_9__), Statistics.create(35.555, 40.551, 46.152), //
				Profile.create(sen_2_5, exp_10_14), Statistics.create(40.107, 45.755, 52.090));
	}

	interface StatTypeFactor {
		double get(Statistics.Type statType);

		default double apply(Statistics.Type statType, Statistics reference) {
			return get(statType) * statType.from(reference);
		}
	}

	interface StatFactor {
		double get(Profile profile, Statistics.Type statType);

		default StatTypeFactor onProfile(Profile profile) {
			return statType -> get(profile, statType);
		}
	}

	private static Map<Profile, Statistics> createFactoredData(Map<Profile, Statistics> data, StatFactor factor) {
		return data.keySet().stream().collect(Collectors.toMap(//
				profile -> profile, //
				profile -> data.get(profile).applyTypeFactor(factor.onProfile(profile))));
	}

	static class ModelGenerator {
		private final RandomGenerator rand;

		public ModelGenerator(RandomGenerator rand) {
			this.rand = rand;
		}

		private Model createDataBasedModel(Map<Profile, Statistics> data) {
			return profile -> rand.gaussian(data.get(profile));
		}
	}

	private static Map<Profile, Collection<Double>> generateSalaries(Model model, Set<Profile> profiles,
			int salariesPerProfile) {
		return profiles.stream()//
				.sorted(bySeniorityThenExperience())// Order to generate same results with same random seed
				.collect(toMap(//
						profile -> profile, //
						profile -> IntStream.range(0, salariesPerProfile)//
								.mapToObj(i -> model.estimateSalary(profile))//
								.collect(toList())));
	}

	interface Period {
		int start();

		int stop();

		static Period createInstant(int value) {
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
	}

	interface Profile {
		Period seniority();

		Period experience();

		static Profile create(Period seniority, Period experience) {
			return new Profile() {

				@Override
				public Period seniority() {
					return seniority;
				}

				@Override
				public Period experience() {
					return experience;
				}

				@Override
				public String toString() {
					return String.format("(sen=%s, exp=%s)", seniority, experience);
				}

				private final Equalable<Profile> equalable = new Equalable<>(Profile.class, this, //
						Profile::seniority, //
						Profile::experience);

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

		default boolean contains(Profile that) {
			return this.seniority().contains(that.seniority()) //
					&& this.experience().contains(that.experience());
		}
	}

	interface Statistics {
		static enum Type {
			Q1(Statistics::Q1), MEAN(Statistics::mean), Q3(Statistics::Q3);

			private final Function<Statistics, Double> method;

			private Type(Function<Statistics, Double> method) {
				this.method = method;
			}

			public double from(Statistics reference) {
				return method.apply(reference);
			}
		};

		double Q1();

		double mean();

		double Q3();

		static Statistics create(double Q1, double mean, double Q3) {
			return new Statistics() {

				@Override
				public double Q1() {
					return Q1;
				}

				@Override
				public double mean() {
					return mean;
				}

				@Override
				public double Q3() {
					return Q3;
				}
			};
		}

		default Statistics applyTypeFactor(StatTypeFactor factor) {
			Statistics stats = this;
			return new Statistics() {

				@Override
				public double Q1() {
					return factor.apply(Type.Q1, stats);
				}

				@Override
				public double mean() {
					return factor.apply(Type.MEAN, stats);
				}

				@Override
				public double Q3() {
					return factor.apply(Type.Q3, stats);
				}
			};
		}
	}

	interface Model {
		double estimateSalary(Profile profile);
	}

	interface Person {
		Profile profile();

		double salary();

		static Person create(Profile profile, double salary) {
			return new Person() {

				@Override
				public Profile profile() {
					return profile;
				}

				@Override
				public double salary() {
					return salary;
				}
			};
		}
	}
}
