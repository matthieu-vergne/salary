package fr.vergne.salary.chart;

import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.error.ErrorBounds;

public interface GraphicalReport {
	void setReferenceStatistics(StatisticsDataset referenceDataset);

	void setModelStatistics(StatisticsDataset modelDataset);

	void setModelBasedSalaries(SalariesDataset salariesDataset, int salariesLimitPerProfile);

	void setSalariesBasedStatistics(StatisticsDataset modelDatasetOnReferenceProfiles);
	
	static GraphicalReport create(String title, int chartWidth, int chartHeight, int transitionWidth) {
		return new JFreeChartReport(title, chartWidth, chartHeight, transitionWidth);
	}

	void setErrorBounds(ErrorBounds q1, ErrorBounds mean, ErrorBounds q3);

}
