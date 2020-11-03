package fr.vergne.salary;

import static fr.vergne.salary.Main.Profile.*;
import static fr.vergne.salary.Main.Period.*;
import static fr.vergne.salary.Main.Statistics.*;
import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class Main {

	public static void main(String[] args) {
		Period sen_0_1 = createPeriod(0, 1);
		Period sen_2_5 = createPeriod(2, 5);

		Period exp_1____ = createInstant(1);
		Period exp_2_5__ = createPeriod(2, 5);
		Period exp_6_9__ = createPeriod(6, 9);
		Period exp_10_14 = createPeriod(10, 14);

		Map<Profile, Statistics> data = Map.of(//
				createProfile(sen_0_1, exp_1____), createStatistics(29.731, 34.062, 38.942), //
				createProfile(sen_0_1, exp_2_5__), createStatistics(31.465, 35.797, 40.640), //
				createProfile(sen_0_1, exp_6_9__), createStatistics(35.429, 40.476, 46.144), //
				createProfile(sen_0_1, exp_10_14), createStatistics(39.964, 45.670, 52.082), //

				createProfile(sen_2_5, exp_1____), createStatistics(29.837, 34.125, 38.948), //
				createProfile(sen_2_5, exp_2_5__), createStatistics(31.577, 35.863, 40.646), //
				createProfile(sen_2_5, exp_6_9__), createStatistics(35.555, 40.551, 46.152), //
				createProfile(sen_2_5, exp_10_14), createStatistics(40.107, 45.755, 52.090));
		Set<Profile> profiles = data.keySet();

		RandomGenerator rand = new RandomGenerator(0);
		@SuppressWarnings("unused")
		Model referenceModel = profile -> rand.gaussian(data.get(profile));

		// TODO Parametered model equivalent to reference model
		// TODO Identify optimal parameters
		// TODO Downhill algorithm to converge parameters to reference
		// TODO GA to converge parameters to reference
		// TODO Parametered model on linear curves
		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves
		Model model = profile -> rand.gaussian(data.get(profile));
		int limitPerProfile = 100000;
		Map<Profile, Collection<Double>> salaries = generateSalaries(profiles, model, limitPerProfile);

		for (Profile profile : profiles) {
			Statistics profileData = data.get(profile);
			StatHelper profileResults = new StatHelper(salaries.get(profile));
			System.out.println(profile);
			displayDiff("Q1", profileResults.Q1(), profileData.Q1());
			displayDiff("moy", profileResults.mean(), profileData.mean());
			displayDiff("Q3", profileResults.Q3(), profileData.Q3());
		}
	}

	private static Map<Profile, Collection<Double>> generateSalaries(Set<Profile> profiles, Model model,
			int limitPerProfile) {
		return profiles.stream().collect(toMap(//
				profile -> profile, //
				profile -> IntStream.range(0, limitPerProfile)//
						.mapToObj(i -> model.estimateSalary(profile))//
						.collect(toList())));
	}

	private static void displayDiff(String id, double actual, double target) {
		double diff = Math.abs(target - actual);
		Double normalisedDiff = StatHelper.normaliseInPercentOf(target).apply(diff);
		System.out.println(String.format("  Δ%s=|%.2f-%.2f|=%.2f%%", id, target, actual, normalisedDiff));
	}

	interface Period {
		int start();

		int stop();

		static Period createInstant(int value) {
			return createPeriod(value, value);
		}

		static Period createPeriod(int start, int stop) {
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
	}

	interface Profile {
		Period seniority();

		Period experience();

		static Profile createProfile(Period seniority, Period experience) {
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
	}

	interface Statistics {
		double Q1();

		double mean();

		double Q3();

		static Statistics createStatistics(double Q1, double mean, double Q3) {
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
	}

	interface Model {
		double estimateSalary(Profile profile);
	}

}
