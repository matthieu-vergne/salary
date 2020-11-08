package fr.vergne.salary.chart;

import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.StatisticsDataset;

public interface GraphicalReport {
	void setReferenceStatistics(StatisticsDataset referenceDataset);

	void setModelStatistics(StatisticsDataset modelDataset);

	void setModelBasedSalaries(SalariesDataset salariesDataset);

	void setSalariesBasedStatistics(StatisticsDataset modelDatasetOnReferenceProfiles);
	
	static GraphicalReport create(int chartWidth, int chartHeight, int transitionWidth) {
		return new JFreeChartReport(chartWidth, chartHeight, transitionWidth);
	}

}
