package fr.vergne.salary.evaluation;

public class ErrorBounds {
	private final double min;
	private final double max;

	private ErrorBounds(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public static ErrorBounds createLargest() {
		return new ErrorBounds(Double.POSITIVE_INFINITY, 0);
	}

	public ErrorBounds refine(double error) {
		return new ErrorBounds(Math.min(min, error), Math.max(max, error));
	}

	@Override
	public String toString() {
		return String.format("[%.2f%% ; %.2f%%]", 100 * min, 100 * max);
	}

}
