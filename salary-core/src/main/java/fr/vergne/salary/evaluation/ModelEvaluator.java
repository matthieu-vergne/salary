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
import fr.vergne.salary.salary.SalaryEstimator;
import fr.vergne.salary.salary.SalaryEstimatorFactory;

public interface ModelEvaluator {
	double evaluate(Model<?> model);

	public interface EvaluationListenerFactory {

		EvaluationListener create(Model<?> model);

	}

	public interface EvaluationListener {
		void salariesCreated(SalariesDataset modelBasedSalaries);

		void salariesStatisticsMeasured(StatisticsDataset salariesStatistics);

		void evaluate(Profile profile, Type type, double actual, double target, double error);

		void modelEvaluated(double score);
	}

	public static ModelEvaluator create(StatisticsDataset referenceStatistics, int randomSeed, int salariesPerProfile,
			EvaluationListenerFactory listenerFactory) {
		SalaryEstimatorFactory salaryEstimatorFactory = new SalaryEstimatorFactory(randomSeed);
		return new ModelEvaluator() {

			@Override
			public double evaluate(Model<?> model) {
				EvaluationListener modelEvaluationListener = listenerFactory.create(model);

				StatisticsDataset modelDataset = model.dataset();
				Set<Profile> modelProfiles = modelDataset.toMap().keySet();
				SalaryEstimator salaryEstimator = salaryEstimatorFactory.createFromStatistics(modelDataset);
				SalariesDataset modelBasedSalaries = salaryEstimator.createSalariesDataset(modelProfiles,
						salariesPerProfile);
				modelEvaluationListener.salariesCreated(modelBasedSalaries);

				Set<Profile> referenceProfiles = referenceStatistics.toMap().keySet();
				StatisticsDataset salariesStatistics = measureSalariesStatistics(modelBasedSalaries, referenceProfiles);
				modelEvaluationListener.salariesStatisticsMeasured(salariesStatistics);

				double[] maxError = { Double.NEGATIVE_INFINITY };
				DiffConsumer consumer = (profile, type, actual, target) -> {
					double error = computeError(actual, target);
					maxError[0] = Math.max(error, maxError[0]);
					modelEvaluationListener.evaluate(profile, type, actual, target, error);
				};
				compareStatistics(salariesStatistics, referenceStatistics, consumer);
				double score = maxError[0];
				modelEvaluationListener.modelEvaluated(score);

				return score;
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

			private StatisticsDataset measureSalariesStatistics(SalariesDataset salariesDataset,
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