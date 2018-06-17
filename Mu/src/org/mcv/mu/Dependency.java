package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;

public class Dependency {
	
	static class Coords {
		
		/* for Jackson */
		Coords() {	
		}
		
		Coords(String spec) {
			String[] parts = spec.split("\\:");
			this.group = parts[0];
			this.artifact = parts[1];
			if (parts.length > 2) {
				this.version = parts[2];
			}
		}

		String path() {
			if (version == null) {
				return group.replace(".", "/") + "/" + artifact + "/";
			}
			return group.replace(".", "/") + "/" + artifact + "/" + version + "/" + artifact + "-" + version;
		}

		@Override
		public String toString() {
			if (version == null) {
				return group + ":" + artifact;
			}
			return group + ":" + artifact + ":" + version;
		}

		public String toKey() {
			return group + ":" + artifact;
		}

		String group;
		String artifact;
		String version;
	}


	public Dependency() {}
	public Dependency(Coords coords) {
		this.coords = coords;
	}
	
	Coords coords;
	List<Dependency> dependencies = new ArrayList<>();
}
