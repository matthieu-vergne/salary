package fr.vergne.salary;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import fr.vergne.salary.SearchAlgorithm.IterationListener;
import fr.vergne.salary.data.Period;
import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.Statistics.Type;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ErrorBounds;
import fr.vergne.salary.evaluation.ErrorBoundsOperators;
import fr.vergne.salary.model.AffineModelOperators;
import fr.vergne.salary.model.Model;
import fr.vergne.salary.model.ReferenceFactorModelOperators;
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

		System.out.println("Create reference statistics");
		StatisticsDataset referenceStatistics = createReferenceDataset();

		ReferenceFactorModelOperators referenceFactorModelOperators = new ReferenceFactorModelOperators(//
				new Random(randomSeed), //
				referenceStatistics);
		AffineModelOperators affineModelOperators = new AffineModelOperators(//
				new Random(randomSeed), //
				referenceStatistics);

		ErrorBoundsOperators errorBoundsOperators = new ErrorBoundsOperators(//
				randomSeed, //
				salariesPerProfileInEvaluation, //
				referenceStatistics);
		Function<ErrorBounds, String> scoreFormatter = errorBoundsOperators.scoreFormatter();

		GraphicalReport<ErrorBounds> report = GraphicalReport.create(//
				reportTitle, //
				chartWidth, chartHeight, transitionWidth, //
				scoreFormatter, salariesPerProfileInReport);
		ReportUpdater<ErrorBounds> reportUpdater = new ReportUpdater<>(report);
		report.setReferenceStatistics(referenceStatistics);

		var modelOperators = affineModelOperators;
		var evaluationOperators = errorBoundsOperators;
		evaluationOperators.register(reportUpdater::createModelEvaluationListener);

		// TODO Parametered model on exponential curves
		// TODO Parametered model on logarithmic curves
		// TODO GA to converge parameters to reference
		SearchAlgorithm algorithm = SearchAlgorithm.createDownHill(//
				modelOperators.parameterGenerator(), //
				modelOperators.parameterAdapter(), //
				modelOperators.modelFactory(), //
				evaluationOperators.modelEvaluator(), //
				evaluationOperators.scoreComparator(), //
				evaluationOperators.scoreFormatter(), //
				createStepDisplayer(scoreFormatter, System.out, reportUpdater));
		while (true) {// TODO Export loop control to report so we can dispose on close
			algorithm.iterate();
		}
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

	static class ReportPreparator<S> implements ErrorBoundsOperators.EvaluationListener<S> {

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
				errorBounds.put(type, ErrorBounds.undefined());
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
			errorBounds.put(type, errorBounds.get(type).extendsTo(error));
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

		public ErrorBoundsOperators.EvaluationListener<S> createModelEvaluationListener(Model<?> model) {
			preparator = new ReportPreparator<S>(report, model);
			return preparator;
		}

		public void updateReportModel(Model<?> model) {
			System.out.println("Update model in report");
			preparator.applyModelToReport();
		}
	}

}
