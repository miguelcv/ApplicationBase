package org.mcv.mu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.experimental.Delegate;

public class Attributes {
	private static final String PUBLIC = "public";
	private static final String LOCAL = "local";
	private static final String PROP = "prop";
	private static final String OWN = "own";
	private static final String MIXIN = "mixin";
	private static final String THUNK = "thunk";
	
	@Delegate Map<String, Object> attr = new HashMap<>();

	public boolean isPublic() {
		boolean val = (Boolean)attr.getOrDefault(LOCAL, true);
		return (Boolean)attr.getOrDefault(PUBLIC, !val);
	}

	public boolean isProp() {
		return (Boolean)attr.getOrDefault(PROP, false);
	}

	public boolean isOwn() {
		return (Boolean)attr.getOrDefault(OWN, false);
	}

	public boolean isLocal(boolean def) {
		return (Boolean)attr.getOrDefault(LOCAL, def);
	}
	
	public boolean isMixin() {
		return (Boolean)attr.getOrDefault(MIXIN, false);
	}

	public boolean isThunk() {
		return (Boolean)attr.getOrDefault(THUNK, false);
	}

	public List<String> attributes() {
		return new ArrayList<>(attr.keySet());
	}
}
