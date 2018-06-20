package org.mcv.utils;

import lombok.Data;

@Data
public class Tuple<S1, S2> {
	private final S1 first;
	private final S2 second;
}
