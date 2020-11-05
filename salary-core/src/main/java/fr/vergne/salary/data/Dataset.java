package fr.vergne.salary.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public interface Dataset<T> {
	Map<Profile, T> toMap();

	static <T> Map<Profile, T> filterMapOnProfile(Map<Profile, T> data, Profile profile) {
		return data.entrySet().stream()//
				.filter(entry -> profile.contains(entry.getKey()))//
				.collect(Collectors.toMap(//
						Entry<Profile, T>::getKey, //
						Entry<Profile, T>::getValue));
	}

}
