package fr.vergne.salary.evaluation;

import static java.lang.Math.*;

public class ErrorBounds {
	final double min;
	final double max;

	private ErrorBounds(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public static ErrorBounds createLargest() {
		return new ErrorBounds(Double.POSITIVE_INFINITY, 0);
	}

	public ErrorBounds refine(double error) {
		return new ErrorBounds(min(min, error), max(max, error));
	}

	@Override
	public String toString() {
		return String.format("[%.2f%% ; %.2f%%]", 100 * min, 100 * max);
	}

}
