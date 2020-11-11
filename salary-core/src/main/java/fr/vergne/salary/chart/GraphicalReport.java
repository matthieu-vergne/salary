package fr.vergne.salary.chart;

import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ErrorBounds;
import fr.vergne.salary.evaluation.ModelEvaluation;

public interface GraphicalReport {
	void setReferenceStatistics(StatisticsDataset referenceDataset);

	void setModelStatistics(StatisticsDataset modelDataset);

	void setModelBasedSalaries(SalariesDataset salariesDataset, int salariesLimitPerProfile);

	void setSalariesBasedStatistics(StatisticsDataset modelDatasetOnReferenceProfiles);

	default void setModelEvaluation(ModelEvaluation evaluation, int salariesPerProfile) {
		this.setModelBasedSalaries(evaluation.modelBasedSalaries(), salariesPerProfile);
		this.setSalariesBasedStatistics(evaluation.salariesStatistics());
		this.setErrorBounds(//
				evaluation.q1ErrorBounds(), //
				evaluation.meanErrorBounds(), //
				evaluation.q3ErrorBounds());
	}

	static GraphicalReport create(String title, int chartWidth, int chartHeight, int transitionWidth) {
		return new JFreeChartReport(title, chartWidth, chartHeight, transitionWidth);
	}

	void setErrorBounds(ErrorBounds q1, ErrorBounds mean, ErrorBounds q3);

}
