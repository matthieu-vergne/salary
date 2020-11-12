package fr.vergne.salary.report;

import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.stream.Collectors.*;
import static javax.swing.KeyStroke.*;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import fr.vergne.salary.data.Period;
import fr.vergne.salary.data.Profile;
import fr.vergne.salary.data.SalariesDataset;
import fr.vergne.salary.data.Statistics;
import fr.vergne.salary.data.StatisticsDataset;
import fr.vergne.salary.evaluation.ErrorBounds;
import fr.vergne.salary.model.Model;

public class JFreeChartReport implements GraphicalReport {

	private static final String SCORE_KEY = "{score}";

	private final int chartWidth;
	private final int chartHeight;
	private final int transitionWidth;
	private final int salariesPerProfile;
	
	private final Function<Double, String> scoreFormatter;

	private final JPanel referenceStatsPanel = createChartPanel();
	private final JPanel modelStatsPanel = createChartPanel();
	private final JPanel modelSalariesPanel = createChartPanel();
	private final JPanel salariesStatsPanel = createChartPanel();

	private final JLabel referenceToModelLabel = createTransitionTextLabel("Model");
	private final JLabel modelToSalariesLabel = createTransitionTextLabel("Generate");
	private final JLabel salariesToStatsLabel = createTransitionTextLabel("Stats");
	private final JLabel referenceComparisonLabel = createTransitionTextLabel("Compare");


	public JFreeChartReport(String title, int chartWidth, int chartHeight, int transitionWidth,
			Function<Double, String> scoreFormatter, int salariesPerProfile) {
		this.chartWidth = chartWidth;
		this.chartHeight = chartHeight;
		this.transitionWidth = transitionWidth;
		this.salariesPerProfile = salariesPerProfile;
		this.scoreFormatter = scoreFormatter;
		createAndShowFrame(title);
	}

	@Override
	public void setReferenceStatistics(StatisticsDataset dataset) {
		String title = "Reference";
		JFreeChart chart = createBoxPlot(dataset, title);
		updateChart(referenceStatsPanel, chart);
	}

	@Override
	public void setModelStatistics(Model<?> model) {
		String title = model.toString();
		JFreeChart chart = createBoxPlot(model.dataset(), title);
		updateChart(modelStatsPanel, chart);
	}

	@Override
	public void setModelBasedSalaries(SalariesDataset dataset) {
		String title = "Model-based Salaries (" + salariesPerProfile + "/profile)";
		JFreeChart chart = createPointsPlot(dataset, title, salariesPerProfile);
		updateChart(modelSalariesPanel, chart);
	}

	@Override
	public void setSalariesBasedStatistics(StatisticsDataset dataset) {
		String title = "Salaries Statistics (all data)";
		JFreeChart chart = createBoxPlot(dataset, title);
		updateChart(salariesStatsPanel, chart);
	}

	@Override
	public void setErrorBounds(ErrorBounds q1, ErrorBounds mean, ErrorBounds q3) {
		referenceComparisonLabel.setText("<html>"//
				+ "<style type='text/css'>"//
				+ "  td {"//
				+ "    text-align: right;"//
				+ "  }"//
				+ "</style>"//
				+ "<body>"//
				+ "<table>"//
				+ "<tr><td>ΔQ1</td><rd>∈</td><td>" + q1 + "</td><td></td></tr>"//
				+ "<tr><td>Δmean</td><rd>∈</td><td>" + mean + "</td><td>=" + SCORE_KEY + "</td></tr>"//
				+ "<tr><td>ΔQ3</td><rd>∈</td><td>" + q3 + "</td><td></td></tr>"//
				+ "</table>"//
				+ "</body>"//
				+ "</html>");
	}

	@Override
	public void setScore(double score) {
		String scoreText = scoreFormatter.apply(score);
		String textTemplate = referenceComparisonLabel.getText();
		String resolvedText = textTemplate.replaceAll(Pattern.quote(SCORE_KEY), scoreText);
		referenceComparisonLabel.setText(resolvedText);
	}

	private void createAndShowFrame(String title) {
		new Thread(() -> {
			JFrame frame = new JFrame();

			/**********************
			 * GENERAL PROPERTIES *
			 **********************/

			frame.setTitle(title);

			int width = 2 * this.chartWidth;
			int height = 2 * this.chartHeight;
			frame.setBounds(0, 0, width, height);

			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			frame.setLayout(new GridBagLayout());
			/********************
			 * CHART CONTAINERS *
			 ********************/

			GridBagConstraints constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.BOTH;
			constraints.weightx = chartWidth;
			constraints.weighty = chartHeight;

			constraints.gridx = 0;
			constraints.gridy = 0;
			frame.add(referenceStatsPanel, constraints);

			constraints.gridx = 2;
			constraints.gridy = 0;
			frame.add(modelStatsPanel, constraints);

			constraints.gridx = 2;
			constraints.gridy = 2;
			frame.add(modelSalariesPanel, constraints);

			constraints.gridx = 0;
			constraints.gridy = 2;
			frame.add(salariesStatsPanel, constraints);

			/*************************
			 * TRANSITION CONTAINERS *
			 *************************/

			constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.BOTH;
			Supplier<LayoutManager> verticalLayout = () -> new GridLayout(2, 1);
			Supplier<LayoutManager> horizontalLayout = () -> new GridLayout(1, 2);
			int alignCenter = SwingConstants.CENTER;
			int alignTop = SwingConstants.TOP;
			int alignBottom = SwingConstants.BOTTOM;
			int alignLeft = SwingConstants.LEFT;
			int alignRight = SwingConstants.RIGHT;

			constraints.weightx = transitionWidth;
			constraints.weighty = chartHeight;
			constraints.gridx = 1;

			constraints.gridy = 0;
			frame.add(//
					createTransitionPanel(//
							verticalLayout, //
							createTransitionArrowLabel("→", alignCenter, alignBottom), //
							configureTransitionTextLabel(referenceToModelLabel, alignCenter, alignTop)), //
					constraints);

			constraints.gridy = 2;
			frame.add(//
					createTransitionPanel(//
							verticalLayout, //
							configureTransitionTextLabel(salariesToStatsLabel, alignCenter, alignBottom), //
							createTransitionArrowLabel("←", alignCenter, alignTop)), //
					constraints);

			constraints.weightx = chartWidth;
			constraints.weighty = transitionWidth;
			constraints.gridy = 1;

			constraints.gridx = 0;
			frame.add(//
					createTransitionPanel(//
							horizontalLayout, //
							createTransitionArrowLabel("↑", alignRight, alignCenter), //
							configureTransitionTextLabel(referenceComparisonLabel, alignLeft, alignCenter)), //
					constraints);

			constraints.gridx = 2;
			frame.add(//
					createTransitionPanel(//
							horizontalLayout, //
							configureTransitionTextLabel(modelToSalariesLabel, alignRight, alignCenter), //
							createTransitionArrowLabel("↓", alignLeft, alignCenter)), //
					constraints);

			/***********
			 * ACTIONS *
			 ***********/

			setKeyboardShortcut(frame, //
					getKeyStroke(VK_S, CTRL_DOWN_MASK), //
					event -> snapshot(frame, title));

			/***********
			 * DISPLAY *
			 ***********/

			frame.setVisible(true);

		}).run();
	}

	private void setKeyboardShortcut(JFrame frame, KeyStroke keyStroke, Consumer<ActionEvent> actionConsumer) {
		@SuppressWarnings("serial")
		AbstractAction action = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent event) {
				actionConsumer.accept(event);
			}
		};
		Object key = keyStroke;
		frame.getRootPane().getActionMap().put(key, action);
		frame.getRootPane().getInputMap().put(keyStroke, key);
	}

	private void snapshot(JFrame frame, String title) {
		Component component = frame.getContentPane();
		BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		// call the Component's paint method, using
		// the Graphics object of the image.
		component.paint(image.getGraphics()); // alternately use .printAll(..)
		File file;
		String name = title.replaceAll("[^a-zA-Z0-9]+", "-");
		try {
			file = File.createTempFile(name + "_", ".png");
		} catch (IOException cause) {
			throw new RuntimeException(cause);
		}
		System.out.println("Save frame in " + file);
		try {
			ImageIO.write(image, "png", file);
		} catch (IOException cause) {
			// TODO Auto-generated catch block
			cause.printStackTrace();
		}
	}

	private JPanel createTransitionPanel(Supplier<LayoutManager> layout, JLabel firstLabel, JLabel secondLabel) {
		JPanel panel = new JPanel(layout.get());
		panel.add(firstLabel);
		panel.add(secondLabel);
		return panel;
	}

	private JLabel configureTransitionTextLabel(JLabel label, int horizontalAlignment, int verticalAlignment) {
		label.setFont(label.getFont()//
				.deriveFont(AffineTransform.getScaleInstance(2, 2)));
		label.setHorizontalAlignment(horizontalAlignment);
		label.setVerticalAlignment(verticalAlignment);
		return label;
	}

	private JLabel createTransitionArrowLabel(String arrow, int horizontalAlignment, int verticalAlignment) {
		JLabel label = new JLabel(arrow);
		label.setFont(label.getFont()//
				.deriveFont(AffineTransform.getScaleInstance(5, 5)));
		label.setHorizontalAlignment(horizontalAlignment);
		label.setVerticalAlignment(verticalAlignment);
		return label;
	}

	private JLabel createTransitionTextLabel(String text) {
		return new JLabel(text);
	}

	private JPanel createChartPanel() {
		return new JPanel(new GridLayout(1, 1));
	}

	private void updateChart(JPanel panel, JFreeChart chart) {
		panel.removeAll();
		panel.add(new ChartPanel(chart));
		panel.revalidate();
	}

	private JFreeChart createBoxPlot(StatisticsDataset statisticsDataset, String chartTitle) {
		Map<Profile, Statistics> map = statisticsDataset.toMap();
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		map.keySet().stream()//
				.sorted(Profile.bySeniorityThenExperience())// Sort currently not managed by JFreeChart
				.forEach(profile -> {
					Statistics statistics = map.get(profile);

					Period seniority = profile.seniority();
					Period experience = profile.experience();

					double q1 = statistics.q1();
					double mean = statistics.mean();
					double q3 = statistics.q3();
					Number median = mean;// Required to work correctly
					Number minRegularValue = q1;// Required to work correctly
					Number maxRegularValue = q3;// Required to work correctly
					Number minOutlier = null;
					Number maxOutlier = null;
					List<Object> outliers = null;

					Comparator<Period> periodComparator = Period.byStartThenStop();
					Comparable<Period> comparableSeniority = createComparable(seniority, periodComparator,
							"Seniority ");
					Comparable<Period> comparableExperience = createComparable(experience, periodComparator);
					BoxAndWhiskerItem item = new BoxAndWhiskerItem(//
							mean, median, q1, q3, //
							minRegularValue, maxRegularValue, //
							minOutlier, maxOutlier, outliers);
					dataset.add(item, comparableSeniority, comparableExperience);
				});

		String categoryAxisLabel = "Experience";
		String valueAxisLabel = "Salary (k€)";
		boolean legend = true;
		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(//
				chartTitle, //
				categoryAxisLabel, valueAxisLabel, //
				dataset, //
				legend);
		return chart;
	}

	private JFreeChart createPointsPlot(SalariesDataset salariesDataset, String chartTitle,
			int salariesLimitPerProfile) {
		DefaultXYDataset dataset = new DefaultXYDataset();
		salariesDataset.toMap().entrySet().stream()//
				.sorted(byExperience())// Sort currently not managed by JFreeChart
				.map(toReducedSalariesSets(salariesLimitPerProfile))//
				.map(toEntriesWithExperienceMovedFromKeyToValue())//
				.collect(intoMapAggregatingExperiencesAndSalariesForEachSeniority())//
				.entrySet().stream()//
				.sorted(bySeniority())// Sort currently not managed by JFreeChart
				.forEach(entry -> {
					Period seniority = entry.getKey();
					Comparator<Period> periodComparator = Period.byStartThenStop();
					Comparable<Period> comparableSeniority = createComparable(seniority, periodComparator,
							"Seniority ");

					List<Entry<Period, Double>> experienceSalaryPairs = entry.getValue().collect(toList());
					int[] index = { 0 };
					double[][] data = { //
							// X
							new double[experienceSalaryPairs.size()], //
							// Y
							new double[experienceSalaryPairs.size()] //
					};
					experienceSalaryPairs.forEach(experienceSalary -> {
						Period experience = experienceSalary.getKey();
						double salary = experienceSalary.getValue();

						// X
						data[0][index[0]] = experience.start();
						// Y
						data[1][index[0]] = salary;

						index[0]++;
					});
					dataset.addSeries(comparableSeniority, data);
				});

		String xAxisLabel = "Experience";
		String yAxisLabel = "Salary (k€)";
		return ChartFactory.createScatterPlot(//
				chartTitle, //
				xAxisLabel, yAxisLabel, //
				dataset);
	}

	private Collector<Entry<Period, Stream<Entry<Period, Double>>>, ?, Map<Period, Stream<Entry<Period, Double>>>> intoMapAggregatingExperiencesAndSalariesForEachSeniority() {
		return Collectors.toMap(//
				Entry<Period, Stream<Entry<Period, Double>>>::getKey, //
				Entry<Period, Stream<Entry<Period, Double>>>::getValue, //
				Stream::concat);
	}

	private Comparator<Entry<Period, Stream<Entry<Period, Double>>>> bySeniority() {
		return Comparator.comparing(//
				Entry<Period, Stream<Entry<Period, Double>>>::getKey, //
				Period.byStartThenStop());
	}

	private Function<Entry<Profile, Stream<Double>>, Entry<Period, Stream<Entry<Period, Double>>>> toEntriesWithExperienceMovedFromKeyToValue() {
		return entry -> {
			Profile profile = entry.getKey();
			Period seniority = profile.seniority();
			Period experience = profile.experience();
			Stream<Double> salaries = entry.getValue();
			return Map.entry(seniority, salaries.map(salary -> Map.entry(experience, salary)));
		};
	}

	private Function<Entry<Profile, Collection<Double>>, Entry<Profile, Stream<Double>>> toReducedSalariesSets(
			int salariesLimitPerProfile) {
		return entry -> {
			Profile profile = entry.getKey();
			Collection<Double> salaries = entry.getValue();
			return Map.entry(profile, salaries.stream().limit(salariesLimitPerProfile));
		};
	}

	private Comparator<Entry<Profile, Collection<Double>>> byExperience() {
		return Comparator.comparing(//
				entry -> entry.getKey().experience(), //
				Period.byStartThenStop());
	}

	private static <T> Comparable<T> createComparable(T value, Comparator<T> comparator) {
		return createComparable(value, comparator, "");
	}

	private static <T> Comparable<T> createComparable(T value, Comparator<T> comparator, String prefix) {
		return new ComparableValue<T>(value, comparator, prefix);
	}

	private static class ComparableValue<T> implements Comparable<T> {

		private final T value;
		private final Comparator<T> comparator;
		private final String prefix;

		private ComparableValue(T value, Comparator<T> comparator, String prefix) {
			this.value = value;
			this.comparator = comparator;
			this.prefix = prefix;
		}

		@Override
		public int compareTo(T other) {
			return comparator.compare(value, other);
		}

		@Override
		public String toString() {
			return prefix + value.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof ComparableValue) {
				ComparableValue<?> that = (ComparableValue<?>) obj;
				return this.value.equals(that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}
}
