package org.mcv.mu;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TestMu {

	@Test
	public void test() {
		List<String> list = getMuFiles();
		for(String file : list) {
			System.out.println("Running " + file);
			try {
				Mu.runFileNoExit(file);
			} catch(Throwable e) {
				System.out.println("Error: " + e);
			}
			System.out.println("Press <ENTER> to continue");
			System.out.flush();
			readLine();
		}
		System.out.println("All done.");
	}

	private void readLine() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			br.readLine();
		} catch(Exception e) {
			// ignore
		}
	}
	private List<String> getMuFiles() {
		//System.out.println(new File(".").getAbsolutePath());
		File testDir = new File("test");
		if(testDir.exists() && testDir.isDirectory()) {
			return getMu(testDir);
		} else {
			return List.of();
		}
	}

	private List<String> getMu(File dir) {
		List<String> ret = new ArrayList<>();
		for(File f : dir.listFiles()) {
			if(f.isDirectory()) {
				ret.addAll(getMu(f));
			} else if(f.getName().endsWith(".mu")) {
				ret.add(f.getAbsolutePath());
			}
		}
		return ret;
	}
}
