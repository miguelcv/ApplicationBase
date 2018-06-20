package org.mcv.mu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.mcv.utils.FileUtils;

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

		public boolean check(File file) {
			if(checksum == null || checksum.isEmpty()) {
				checksum = FileUtils.computeHash256(file);
				return true;
			}
			return checksum.equals(FileUtils.computeHash256(file));
		}

		String group;
		String artifact;
		String version;
		String checksum;
	}


	public Dependency() {}
	public Dependency(Coords coords) {
		this.coords = coords;
	}
	
	Coords coords;
	List<Dependency> dependencies = new ArrayList<>();
}
