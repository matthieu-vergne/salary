package fr.vergne.salary.evaluation;

import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.StatisticsDataset;

public interface ModelEvaluation {
	SalariesDataset modelBasedSalaries();

	StatisticsDataset salariesStatistics();

	ErrorBounds q1ErrorBounds();

	ErrorBounds meanErrorBounds();

	ErrorBounds q3ErrorBounds();

	static ModelEvaluation create(//
			SalariesDataset modelBasedSalaries, //
			StatisticsDataset salariesStatistics, //
			ErrorBounds q1ErrorBounds, //
			ErrorBounds meanErrorBounds, //
			ErrorBounds q3ErrorBounds) {
		return new ModelEvaluation() {

			@Override
			public SalariesDataset modelBasedSalaries() {
				return modelBasedSalaries;
			}

			@Override
			public StatisticsDataset salariesStatistics() {
				return salariesStatistics;
			}

			@Override
			public ErrorBounds q1ErrorBounds() {
				return q1ErrorBounds;
			}

			@Override
			public ErrorBounds meanErrorBounds() {
				return meanErrorBounds;
			}

			@Override
			public ErrorBounds q3ErrorBounds() {
				return q3ErrorBounds;
			}

		};
	}
}
