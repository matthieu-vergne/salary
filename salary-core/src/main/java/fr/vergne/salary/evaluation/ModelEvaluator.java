package fr.vergne.salary.evaluation;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.Statistics.Type;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.model.Model;
import fr.vergne.salary.model.ModelFactory;
import fr.vergne.salary.model.RandomFactory;

public interface ModelEvaluator {
	ModelEvaluation evaluate(StatisticsDataset modelStatistics);

	public static ModelEvaluator create(StatisticsDataset referenceStatistics, int salariesPerProfile) {
		return new ModelEvaluator() {

			@Override
			public ModelEvaluation evaluate(StatisticsDataset modelStatistics) {
				System.out.println("  Create salaries from model statistics");
				Set<Profile> modelProfiles = modelStatistics.toMap().keySet();
				Model model = new ModelFactory(new RandomFactory(0))//
						.createDataBasedModel(modelStatistics);
				SalariesDataset modelBasedSalaries = model.createSalariesDataset(modelProfiles, salariesPerProfile);

				System.out.println("  Compute salaries statistics on reference profiles");
				Set<Profile> referenceProfiles = referenceStatistics.toMap().keySet();
				StatisticsDataset salariesStatistics = computeSalariesStatisticsForProfiles(modelBasedSalaries,
						referenceProfiles);

				System.out.println("  Compare salaries statistics to reference statistics");
				ErrorBounds q1ErrorBounds = ErrorBounds.createLargest();
				ErrorBounds meanErrorBounds = ErrorBounds.createLargest();
				ErrorBounds q3ErrorBounds = ErrorBounds.createLargest();
				DiffConsumer consumer = (profile, type, actual, target) -> {
					select(type, q1ErrorBounds, meanErrorBounds, q3ErrorBounds)//
							.refine(computeError(actual, target));
				};
				compareStatistics(salariesStatistics, referenceStatistics, consumer);

				return ModelEvaluation.create(//
						modelBasedSalaries, salariesStatistics, //
						q1ErrorBounds, meanErrorBounds, q3ErrorBounds);
			}

			private double computeError(double actual, double target) {
				return Math.abs(target - actual) / target;
			}

			private void compareStatistics(StatisticsDataset actualDataset, StatisticsDataset targetDataset,
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

			private ErrorBounds select(Type type, ErrorBounds q1ErrorBounds, ErrorBounds meanErrorBounds,
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

			private StatisticsDataset computeSalariesStatisticsForProfiles(SalariesDataset salariesDataset,
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
		};
	}
}

interface DiffConsumer {
	void consume(Profile profile, Type type, double actual, double target);

	default void consume(Profile profile, Type type, Statistics actualStats, Statistics targetStats) {
		consume(profile, type, type.from(actualStats), type.from(targetStats));
	}
}