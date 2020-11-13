package fr.vergne.salary;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.vergne.salary.SearchAlgorithm.IterationListener;
import fr.vergne.salary.data.Period;
import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.Statistics.Type;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ErrorBounds;
import fr.vergne.salary.evaluation.ModelEvaluator;
import fr.vergne.salary.model.Model;
import fr.vergne.salary.report.GraphicalReport;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		String reportTitle = "Salary Analysis Charts";
		int chartWidth = 500;
		int chartHeight = 500;
		int transitionWidth = 50;
		int salariesPerProfileInEvaluation = 1000;
		int salariesPerProfileInReport = 10;
		int randomSeed = 0;
		Function<ErrorBounds, String> scoreFormatter = ErrorBounds::toString;

		GraphicalReport<ErrorBounds> report = GraphicalReport.create(//
				reportTitle, //
				chartWidth, chartHeight, transitionWidth, //
				scoreFormatter, salariesPerProfileInReport);
		ReportUpdater<ErrorBounds> reportUpdater = new ReportUpdater<>(report);

		System.out.println("Create reference statistics");
		StatisticsDataset referenceStatistics = createReferenceDataset();
		report.setReferenceStatistics(referenceStatistics);

		Random rand = new Random(randomSeed);
		// TODO Increase to 1 parameter per stat type
		// TODO Increase to 1 parameter per profile & stat type

		Supplier<StatsAffine> parameterGenerator = () -> StatsAffine.create(//
				Affine.fromSlopeIntercept(1, 1), //
				Affine.fromSlopeIntercept(1, 2), //
				Affine.fromSlopeIntercept(1, 3)//
		);
		Function<StatsAffine, StatsAffine> parameterAdapter = params -> {
			double delta = rand.nextDouble() - 0.5;
			List<StatsAffine> candidates = Stream.of(//
					// Variants
					params.addQ1Start(delta), //
					params.addQ1End(delta), //
					params.addMeanStart(delta), //
					params.addMeanEnd(delta), //
					params.addQ3Start(delta), //
					params.addQ3End(delta), //
					params.addGlobalIntercept(delta) //
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
		Function<StatsAffine, Model<StatsAffine>> modelFactory = params -> {
			String name = "" + params;
			StatisticsDataset dataset = StatisticsDataset.fromMap(referenceStatistics//
					.splitProfiles()//
					.toMap().keySet().stream()//
					.collect(Collectors.toMap(//
							profile -> profile, //
							profile -> {
								int exp = profile.experience().start();
								return Statistics.create(//
										params.q1().resolve(exp), //
										params.mean().resolve(exp), //
										params.q3().resolve(exp));
							})));
			return Model.create(params, name, dataset);
		};
		// FIXME generate salaries with non-symmetric gaussian
		// https://en.wikipedia.org/wiki/Skew_normal_distribution
		Function<Model<StatsAffine>, ErrorBounds> modelEvaluator = ModelEvaluator.create(//
				referenceStatistics, //
				randomSeed, //
				salariesPerProfileInEvaluation, //
				reportUpdater::createModelEvaluationListener)::evaluate;
		Comparator<ErrorBounds> maxComparator = Comparator.comparing(ErrorBounds::max).reversed();
		Comparator<ErrorBounds> minComparator = Comparator.comparing(ErrorBounds::min).reversed();
		int[] failureCounter = { 0 };
		Comparator<ErrorBounds> scoreComparator = maxComparator.thenComparing(minComparator);

		// TODO GA to converge parameters to reference
		// TODO Parametered model on linear curves
		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves
		SearchAlgorithm algorithm = SearchAlgorithm.createDownHill(//
				parameterGenerator, parameterAdapter, //
				modelFactory, //
				modelEvaluator, //
				scoreComparator, scoreFormatter, //
				List.of(//
						createStepDisplayer(scoreFormatter, System.out, reportUpdater), //
						createFailureCounter(failureCounter)//
				));
		while (true) {// TODO Export loop control to report so we can dispose on close
			algorithm.iterate();
		}
	}

	private static IterationListener<StatsAffine, ErrorBounds> createFailureCounter(int[] count) {
		return new IterationListener<StatsAffine, ErrorBounds>() {

			Model<StatsAffine> candidate;

			@Override
			public void startIteration() {
				// Ignore
			}

			@Override
			public void parameterGenerated(StatsAffine parameter) {
				// Ignore
			}

			@Override
			public void modelGenerated(Model<StatsAffine> model) {
				candidate = model;
			}

			@Override
			public void modelScored(Model<StatsAffine> model, ErrorBounds score) {
				// Ignore
			}

			@Override
			public void bestModelSelected(Model<StatsAffine> model) {
				if (model == candidate) {
					count[0] = 0;
				} else {
					count[0]++;
				}
			}
		};
	}

	private static Model<Double> createFactoredReferenceModel(StatisticsDataset referenceStatistics, double factor) {
		String name = "Ref x " + factor;
		StatisticsDataset dataset = referenceStatistics//
				.splitProfiles()//
				.factor((profile, statType) -> factor);
		return Model.create(factor, name, dataset);
	}

	private static <P, S> IterationListener<P, S> createStepDisplayer(Function<S, String> scoreFormatter,
			PrintStream out, ReportUpdater<S> reportUpdater) {
		return new SearchAlgorithm.IterationListener<P, S>() {

			Model<P> bestModel;
			String bestScore;
			Model<P> candidateModel;
			String candidateScore;

			@Override
			public void startIteration() {
				out.println();
			}

			@Override
			public void parameterGenerated(P parameter) {
				// Ignore
			}

			@Override
			public void modelGenerated(Model<P> model) {
				candidateModel = model;
				out.println("New model: " + model);
			}

			@Override
			public void modelScored(Model<P> model, S score) {
				candidateScore = scoreFormatter.apply(score);
				out.println("Score: " + candidateScore);
			}

			@Override
			public void bestModelSelected(Model<P> model) {
				if (model == candidateModel) {
					bestModel = candidateModel;
					bestScore = candidateScore;
					reportUpdater.updateReportModel(bestModel);
				}
				out.println(String.format("Best: %s (%s)", bestModel, bestScore));
			}
		};
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

	static class ReportPreparator<S> implements ModelEvaluator.EvaluationListener<S> {

		private final GraphicalReport<S> report;
		private final Model<?> model;
		private SalariesDataset modelBasedSalaries;
		private StatisticsDataset salariesStatistics;
		private Map<Type, ErrorBounds> errorBounds = new HashMap<>();
		private S score;

		public ReportPreparator(GraphicalReport<S> report, Model<?> model) {
			this.report = report;
			this.model = model;
			Stream.of(Type.values()).forEach(type -> {
				errorBounds.put(type, ErrorBounds.createLargest());
			});
		}

		@Override
		public void salariesCreated(SalariesDataset modelBasedSalaries) {
			this.modelBasedSalaries = modelBasedSalaries;
		}

		@Override
		public void salariesStatisticsMeasured(StatisticsDataset salariesStatistics) {
			this.salariesStatistics = salariesStatistics;
		}

		@Override
		public void evaluate(Profile profile, Type type, double actual, double target, double error) {
			errorBounds.put(type, errorBounds.get(type).refine(error));
		}

		@Override
		public void modelEvaluated(S score) {
			this.score = score;
		}

		void applyModelToReport() {
			report.setModelStatistics(model);
			report.setModelBasedSalaries(modelBasedSalaries);
			report.setSalariesBasedStatistics(salariesStatistics);
			report.setErrorBounds(//
					errorBounds.get(Type.Q1), //
					errorBounds.get(Type.MEAN), //
					errorBounds.get(Type.Q3));
			report.setScore(score);
		}
	};

	static class ReportUpdater<S> {

		private final GraphicalReport<S> report;
		private ReportPreparator<S> preparator;

		public ReportUpdater(GraphicalReport<S> report) {
			this.report = report;
		}

		public ModelEvaluator.EvaluationListener<S> createModelEvaluationListener(Model<?> model) {
			preparator = new ReportPreparator<S>(report, model);
			return preparator;
		}

		public void updateReportModel(Model<?> model) {
			System.out.println("Update model in report");
			preparator.applyModelToReport();
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
			return slope() * x + intercept();
		}

		default Affine addSlope(double delta) {
			return fromSlopeIntercept(slope() + delta, intercept());
		}

		default Affine addIntercept(double delta) {
			return fromSlopeIntercept(slope(), intercept() + delta);
		}

		default Affine movePoint(double delta, int xMove, int xFix) {
			double normDelta = delta / (xFix - xMove);
			return fromSlopeIntercept(//
					this.slope() - normDelta, //
					this.intercept() + xFix * normDelta);
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
					return String.format("[%.1f;%.1f]", start(), end());
				}
			};
		}
	}

	static interface StatsAffine {

		Affine q1();

		Affine mean();

		Affine q3();

		static StatsAffine create(Affine q1, Affine mean, Affine q3) {
			return new StatsAffine() {

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
					return String.format("%s < %s < %s", q1, mean, q3);
				}
			};
		}

		default StatsAffine addGlobalIntercept(double delta) {
			return this.addQ1Intercept(delta).addMeanIntercept(delta).addQ3Intercept(delta);
		}

		default StatsAffine addQ1Slope(double delta) {
			return create(q1().addSlope(delta), mean(), q3());
		}

		default StatsAffine addQ1Intercept(double delta) {
			return create(q1().addIntercept(delta), mean(), q3());
		}

		default StatsAffine addMeanSlope(double delta) {
			return create(q1(), mean().addSlope(delta), q3());
		}

		default StatsAffine addMeanIntercept(double delta) {
			return create(q1(), mean().addIntercept(delta), q3());
		}

		default StatsAffine addQ3Slope(double delta) {
			return create(q1(), mean(), q3().addSlope(delta));
		}

		default StatsAffine addQ3Intercept(double delta) {
			return create(q1(), mean(), q3().addIntercept(delta));
		}

		default StatsAffine addQ1Start(double delta) {
			return create(q1().addStart(delta), mean(), q3());
		}

		default StatsAffine addQ1End(double delta) {
			return create(q1().addEnd(delta), mean(), q3());
		}

		default StatsAffine addMeanStart(double delta) {
			return create(q1(), mean().addStart(delta), q3());
		}

		default StatsAffine addMeanEnd(double delta) {
			return create(q1(), mean().addEnd(delta), q3());
		}

		default StatsAffine addQ3Start(double delta) {
			return create(q1(), mean(), q3().addStart(delta));
		}

		default StatsAffine addQ3End(double delta) {
			return create(q1(), mean(), q3().addEnd(delta));
		}
	}
}
