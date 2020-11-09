package fr.vergne.salary.error;

public class ErrorBounds {
	double min;
	double max;

	private ErrorBounds(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public static ErrorBounds createLargest() {
		return new ErrorBounds(Double.POSITIVE_INFINITY, 0);
	}

	public void refine(double error) {
		min = Math.min(min, error);
		max = Math.max(max, error);
	}

	@Override
	public String toString() {
		return String.format("[%.2f%% ; %.2f%%]", 100 * min, 100 * max);
	}

	public static double computeError(double actual, double target) {
		return Math.abs(target - actual) / target;
	}

}
