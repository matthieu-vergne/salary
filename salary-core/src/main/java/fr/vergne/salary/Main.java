package fr.vergne.salary;

import static fr.vergne.salary.error.ErrorBounds.*;

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
import fr.vergne.salary.error.ErrorBounds;
import fr.vergne.salary.model.ModelFactory;
import fr.vergne.salary.model.RandomFactory;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		int chartWidth = 500;
		int chartHeight = 500;
		int transitionWidth = 50;
		GraphicalReport report = GraphicalReport.create(chartWidth, chartHeight, transitionWidth);

		System.out.println("Create reference statistics");
		StatisticsDataset referenceStatistics = createReferenceDataset();
		Set<Profile> referenceProfiles = referenceStatistics.toMap().keySet();
		report.setReferenceStatistics(referenceStatistics);

		System.out.println("Create model statistics");
		double[] factor = { 1 };
		StatisticsDataset modelStatistics = referenceStatistics.splitProfiles()
				.factor((profile, statType) -> factor[0]);
		report.setModelStatistics(modelStatistics);

		System.out.println("Create salaries from model statistics");
		Set<Profile> modelProfiles = modelStatistics.toMap().keySet();
		int salariesPerProfileInData = 10000;
		SalariesDataset modelBasedSalaries = new ModelFactory(new RandomFactory(0))//
				.createDataBasedModel(modelStatistics)//
				.createSalariesDataset(modelProfiles, salariesPerProfileInData);
		int salariesPerProfileInReport = 10;
		report.setModelBasedSalaries(modelBasedSalaries, salariesPerProfileInReport);

		System.out.println("Compute salaries statistics on reference profiles");
		StatisticsDataset salariesStatistics = computeSalariesStatisticsForProfiles(modelBasedSalaries,
				referenceProfiles);
		report.setSalariesBasedStatistics(salariesStatistics);

		System.out.println("Compare salaries statistics to reference statistics");
		ErrorBounds q1ErrorBounds = ErrorBounds.createLargest();
		ErrorBounds meanErrorBounds = ErrorBounds.createLargest();
		ErrorBounds q3ErrorBounds = ErrorBounds.createLargest();
		DiffConsumer consumer = (profile, type, actual, target) -> {
			select(type, q1ErrorBounds, meanErrorBounds, q3ErrorBounds)//
					.refine(computeError(actual, target));
		};
		compareStatistics(salariesStatistics, referenceStatistics, consumer);
		report.setErrorBounds(q1ErrorBounds, meanErrorBounds, q3ErrorBounds);

		System.out.println("Done");

		// TODO Downhill algorithm to converge factor from 0 to 1
		// TODO Increase to 1 parameter per type
		// TODO Increase to 1 parameter per profile & type
		// TODO GA to converge parameters to reference
		// TODO Parametered model on linear curves
		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves
	}

	private static ErrorBounds select(Type type, ErrorBounds q1ErrorBounds, ErrorBounds meanErrorBounds,
			ErrorBounds q3ErrorBounds) {
		switch (type) {
		case Q1:
			return q1ErrorBounds;
		case MEAN:
			return meanErrorBounds;
		case Q3:
			return q3ErrorBounds;
		default:
			throw new RuntimeException("Unmanaged type: " + type);
		}
	}

	private static void compareStatistics(StatisticsDataset actualDataset, StatisticsDataset targetDataset,
			DiffConsumer consumer) {
		Map<Profile, Statistics> targetData = targetDataset.toMap();
		Map<Profile, Statistics> actualData = actualDataset.toMap();
		targetData.keySet().stream()//
				.sorted(Profile.bySeniorityThenExperience())//
				.forEach(profile -> {
					Statistics actualStats = actualData.get(profile);
					Statistics targetStats = targetData.get(profile);
					for (Type type : Type.values()) {
						consumer.consume(profile, type, actualStats, targetStats);
					}
				});
	}

	private static StatisticsDataset computeSalariesStatisticsForProfiles(SalariesDataset salariesDataset,
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
