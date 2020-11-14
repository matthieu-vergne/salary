package fr.vergne.salary.evaluation;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.Statistics.Type;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.model.Model;
import fr.vergne.salary.salary.SalaryEstimator;
import fr.vergne.salary.salary.SalaryEstimatorFactory;

public class ErrorBoundsOperators {
	private final int randomSeed;
	private final int salariesPerProfileInEvaluation;
	private final StatisticsDataset referenceStatistics;

	private final Collection<EvaluationListenerFactory<ErrorBounds>> listenerFactories = new LinkedList<>();

	public ErrorBoundsOperators(//
			int randomSeed, //
			int salariesPerProfileInEvaluation, //
			StatisticsDataset referenceStatistics) {
		this.randomSeed = randomSeed;
		this.salariesPerProfileInEvaluation = salariesPerProfileInEvaluation;
		this.referenceStatistics = referenceStatistics;
	}

	public void register(EvaluationListenerFactory<ErrorBounds> factory) {
		listenerFactories.add(factory);
	}

	public <P> Function<Model<P>, ErrorBounds> modelEvaluator() {
		SalaryEstimatorFactory salaryEstimatorFactory = new SalaryEstimatorFactory(randomSeed);
		return model -> {
			List<EvaluationListener<ErrorBounds>> listeners = listenerFactories.stream()//
					.map(factory -> factory.create(model))//
					.collect(Collectors.toList());

			StatisticsDataset modelDataset = model.dataset();
			Set<Profile> modelProfiles = modelDataset.toMap().keySet();
			SalaryEstimator salaryEstimator = salaryEstimatorFactory.createFromStatistics(modelDataset);
			SalariesDataset modelBasedSalaries = salaryEstimator.createSalariesDataset(modelProfiles,
					salariesPerProfileInEvaluation);
			listeners.forEach(listener -> listener.salariesCreated(modelBasedSalaries));

			Set<Profile> referenceProfiles = referenceStatistics.toMap().keySet();
			StatisticsDataset salariesStatistics = measureSalariesStatistics(modelBasedSalaries, referenceProfiles);
			listeners.forEach(listener -> listener.salariesStatisticsMeasured(salariesStatistics));

			ErrorBounds[] errorBounds = { ErrorBounds.undefined() };
			DiffConsumer consumer = (profile, type, actual, target) -> {
				double error = computeError(actual, target);
				errorBounds[0] = errorBounds[0].extendsTo(error);
				listeners.forEach(listener -> listener.evaluate(profile, type, actual, target, error));
			};
			compareStatistics(salariesStatistics, referenceStatistics, consumer);
			ErrorBounds score = errorBounds[0];
			listeners.forEach(listener -> listener.modelEvaluated(score));

			return score;
		};
	}

	public Comparator<ErrorBounds> scoreComparator() {
		Comparator<ErrorBounds> maxComparator = Comparator.comparing(ErrorBounds::max).reversed();
		Comparator<ErrorBounds> minComparator = Comparator.comparing(ErrorBounds::min).reversed();
		return maxComparator.thenComparing(minComparator);
	}
	
	public Function<ErrorBounds, String> scoreFormatter() {
		return ErrorBounds::toString;
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

	private StatisticsDataset measureSalariesStatistics(SalariesDataset salariesDataset, Set<Profile> profiles) {
		return StatisticsDataset.fromMap(profiles.stream()//
				.map(profile -> {
					Statistics statistics = salariesDataset.filterOnProfile(profile).toStatistics();
					return Map.entry(profile, statistics);
				})//
				.collect(Collectors.toMap(//
						Entry<Profile, Statistics>::getKey, //
						Entry<Profile, Statistics>::getValue)));
	}

	public interface EvaluationListenerFactory<S> {

		EvaluationListener<S> create(Model<?> model);

	}

	public interface EvaluationListener<S> {
		void salariesCreated(SalariesDataset modelBasedSalaries);

		void salariesStatisticsMeasured(StatisticsDataset salariesStatistics);

		void evaluate(Profile profile, Type type, double actual, double target, double error);

		void modelEvaluated(S score);
	}

	interface DiffConsumer {
		void consume(Profile profile, Type type, double actual, double target);

		default void consume(Profile profile, Type type, Statistics actualStats, Statistics targetStats) {
			consume(profile, type, type.from(actualStats), type.from(targetStats));
		}
	}
}
