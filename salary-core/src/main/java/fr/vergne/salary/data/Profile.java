package fr.vergne.salary.data;

import java.util.Comparator;

import fr.vergne.salary.util.Equalable;

public interface Profile {
	Period seniority();

	Period experience();

	static Profile create(Period seniority, Period experience) {
		return new Profile() {

			@Override
			public Period seniority() {
				return seniority;
			}

			@Override
			public Period experience() {
				return experience;
			}

			@Override
			public String toString() {
				return String.format("(sen=%s, exp=%s)", seniority, experience);
			}

			private final Equalable<Profile> equalable = new Equalable<>(Profile.class, this, //
					Profile::seniority, //
					Profile::experience);

			@Override
			public boolean equals(Object obj) {
				return equalable.equals(obj);
			}

			@Override
			public int hashCode() {
				return equalable.hashCode();
			}
		};
	}

	default boolean contains(Profile that) {
		return this.seniority().contains(that.seniority()) //
				&& this.experience().contains(that.experience());
	}
	
	public static Comparator<Profile> bySeniorityThenExperience() {
		Comparator<Period> startComparator = Comparator.comparing(Period::start);
		Comparator<Period> stopComparator = Comparator.comparing(Period::stop);
		Comparator<Period> periodComparator = startComparator.thenComparing(stopComparator);

		Comparator<Profile> seniorityComparator = Comparator.comparing(Profile::seniority, periodComparator);
		Comparator<Profile> experienceComparator = Comparator.comparing(Profile::experience, periodComparator);
		Comparator<Profile> profileComparator = seniorityComparator.thenComparing(experienceComparator);

		return profileComparator;
	}
}
