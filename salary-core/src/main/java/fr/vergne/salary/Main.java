package fr.vergne.salary;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import fr.vergne.salary.chart.GraphicalReport;
import fr.vergne.salary.data.Period;
import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.Statistics.Type;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.model.Model;
import fr.vergne.salary.model.ModelFactory;
import fr.vergne.salary.model.RandomFactory;

public class Main {

	public static void main(String[] args) throws InterruptedException {

		System.out.println("Create reference data");
		StatisticsDataset referenceDataset = createReferenceDataset();
		int chartWidth = 500;
		int chartHeight = 500;
		int transitionWidth = 50;
		GraphicalReport report = GraphicalReport.create(chartWidth, chartHeight, transitionWidth);
		report.setReferenceStatistics(referenceDataset);

		System.out.println("Create model");
		double[] factor = { 1 };
		StatisticsDataset modelDataset = referenceDataset.splitProfiles().factor((profile, statType) -> factor[0]);
		report.setModelStatistics(modelDataset);

		ModelFactory modelGenerator = new ModelFactory(new RandomFactory(0));
		Model model = modelGenerator.createDataBasedModel(modelDataset);

		// TODO Downhill algorithm to converge factor from 0 to 1
		// TODO Increase to 1 parameter per type
		// TODO Increase to 1 parameter per profile & type
		// TODO GA to converge parameters to reference
		// TODO Parametered model on linear curves
		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves

		System.out.println("Create salaries from model");
		int salariesPerProfile = 10000;
		Set<Profile> modelProfiles = modelDataset.toMap().keySet();
		SalariesDataset salariesDataset = model.createSalariesDataset(modelProfiles, salariesPerProfile);
		report.setModelBasedSalaries(salariesDataset);

		System.out.println("Compute model statistics on reference profiles");
		StatisticsDataset modelDatasetOnReferenceProfiles = computeStatisticsForProfiles(salariesDataset,
				referenceDataset.toMap().keySet());
		report.setSalariesBasedStatistics(modelDatasetOnReferenceProfiles);
		
		// TODO Add error rates to report
		if (1 == 1)// TODO remove
			return;

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
		compareToTarget(salariesDataset, modelDataset, updateErrorBounds);
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
		compareToTarget(salariesDataset, referenceDataset, (profile, type, actual, target) -> {
			updateErrorBounds.consume(profile, type, actual, target);
			// displayDetails.consume(profile, type, actual, target);
		});
		displayGlobalErrorBounds(errorBoundsMap);

		System.out.println("Done");
	}

	private static StatisticsDataset computeStatisticsForProfiles(SalariesDataset salariesDataset,
			Set<Profile> profiles) {
		return StatisticsDataset.fromMap(profiles.stream()//
				.map(profile -> {
					Statistics statistics = salariesDataset.filterOnProfile(profile).toStatistics();
					return Map.entry(profile, statistics);
				})//
				.collect(Collectors.toMap(//
						Entry<Profile, Statistics>::getKey, //
						Entry<Profile, Statistics>::getValue)));
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

	private static void compareToTarget(SalariesDataset actualDataset, StatisticsDataset targetDataset,
			DiffConsumer consumer) {
		Map<Profile, Statistics> targetData = targetDataset.toMap();

		targetData.keySet().stream()//
				.sorted(Profile.bySeniorityThenExperience())//
				.forEach(profile -> {
					Statistics actualStats = actualDataset.filterOnProfile(profile).toStatistics();
					Statistics targetStats = targetData.get(profile);
					for (Type type : Type.values()) {
						consumer.consume(profile, type, actualStats, targetStats);
					}
				});
		;
	}

	interface DiffConsumer {
		void consume(Profile profile, Type type, double actual, double target);

		default void consume(Profile profile, Type type, Statistics actualStats, Statistics targetStats) {
			consume(profile, type, type.from(actualStats), type.from(targetStats));
		}
	}

	private static StatisticsDataset createReferenceDataset() {
		Period sen_0_1 = Period.create(0, 1);
		Period sen_2_5 = Period.create(2, 5);

		Period exp_1____ = Period.create(1);
		Period exp_2_5__ = Period.create(2, 5);
		Period exp_6_9__ = Period.create(6, 9);
		Period exp_10_14 = Period.create(10, 14);

		return StatisticsDataset.fromMap(Map.of(//
				Profile.create(sen_0_1, exp_1____), Statistics.create(29.731, 34.062, 38.942), //
				Profile.create(sen_0_1, exp_2_5__), Statistics.create(31.465, 35.797, 40.640), //
				Profile.create(sen_0_1, exp_6_9__), Statistics.create(35.429, 40.476, 46.144), //
				Profile.create(sen_0_1, exp_10_14), Statistics.create(39.964, 45.670, 52.082), //

				Profile.create(sen_2_5, exp_1____), Statistics.create(29.837, 34.125, 38.948), //
				Profile.create(sen_2_5, exp_2_5__), Statistics.create(31.577, 35.863, 40.646), //
				Profile.create(sen_2_5, exp_6_9__), Statistics.create(35.555, 40.551, 46.152), //
				Profile.create(sen_2_5, exp_10_14), Statistics.create(40.107, 45.755, 52.090)));
	}

}
