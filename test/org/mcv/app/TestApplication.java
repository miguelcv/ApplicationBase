package org.mcv.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestApplication {

	@BeforeClass
	public static void setupClass() {
		clearDirectory(new File("./logs"));		
	}

	private static void clearDirectory(File dir) {
		for(File f : dir.listFiles()) {
			if(f.isDirectory()) {
				clearDirectory(f);
			}
			f.delete();
		}
	}

	private Application mkApp() {
		Application app = new Application("app");
		app.clear();
		return app;
	}
	
	/* app should initialize DB */
	@Test
	public void testApplicationDB() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		} finally {
			app.getDb().close();
		}
	}

	/* app should be able to create and persist and retrieve Base objects */
	@Test
	public void testApplicationBaseCreate() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			// create and persist
			Base base = app.create("base", Base.class);
			for(LogEntry entry : app.log.getLogs()) {
				System.out.println(entry);
			}
			assertTrue(base != null);
			//System.out.println("created: " + base.toString());
			// retrieve
			Base copy = app.create("base", Base.class);
			//System.out.println("retrieved: " + copy.toString());
			assertTrue(base.equals(copy));
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}
	}

	/* app should be able to create and persist and retrieve Base subclass objects */
	@Test
	public void testApplicationBaseSubCreate() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			// create and persist
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			//System.out.println(base.toString());
			// retrieve
			BaseSub copy = app.create("base", BaseSub.class);
			//System.out.println(copy.toString());
			assertTrue(base.equals(copy));
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}
	}

	/* app should not be be able to retrieve deleted Base objects */
	@Test
	public void testApplicationBaseDeleted() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			// create and persist
			Base base = app.create("base", Base.class);
			assertTrue(base != null);
			//System.out.println("created: " + base.toString());
			base.delete();
			//System.out.println("deleted: " + base.toString());
			// retrieve
			Base copy = app.create("base", Base.class);
			if(copy != null) {
				System.out.println("deleted: " + copy.toString());
			}
			assertTrue(copy == null);
			base.undelete();
			copy = app.create("base", Base.class);
			assertTrue(copy.equals(base));
			
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}
	}

	/* app should be able to retrieve current version of changed subclass objects */
	@Test
	public void testApplicationBaseChanged() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			// create and persist
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			//System.out.println("created: " + base.toString());
			base.setMsg("Changed");
			//System.out.println("deleted: " + base.toString());
			// retrieve
			BaseSub copy = app.create("base", BaseSub.class);
			assertTrue(copy.equals(base));

		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}
	}

	// base get by class => list
	@Test
	public void testApplicationGetList() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			List<BaseSub> list1 = new ArrayList<>();
			for(int i=0; i < 10; i++) {
				// create and persist
				BaseSub base = app.create("base" + i, BaseSub.class);
				assertTrue(base != null);
				list1.add(base);
			}
			for(int i=0; i < 10; i++) {
				// change and persist
				BaseSub base = list1.get(i);
				base.setMsg("Changed"+i);
			}
			// retrieve
			List<BaseSub> list2 = app.getList(BaseSub.class);

			assertEquals(10, list2.size());
			for(int i=0; i < 10; i++) {
				BaseSub base1 = list1.get(i);
				System.out.println(base1);
				BaseSub base2 = list2.get(i);
				System.out.println(base2);
				assertTrue(base1.equals(base2));				
			}
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}
	}
	
	@Test
	public void testGetLogs() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			base.setMsg("Changed");
			base.setMsg("Changed again");
			base.setMsg("Changed yet again");
			base.setMsg("Keep on changing");
			base.toString();
			base.debug("This is a debug message");
			base.info("This is an info message");
			base.warn(new Exception("Oops"), "This is a warning message");
			base.error(new Exception("Really bad"), "This is an error message");
			assertTrue(base.getVersion() == 5);
			List<LogEntry> logs = base.getLogs();
			for(LogEntry logentry : logs) {
				System.out.println(logentry);
			}
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}		
	}

	@Test
	public void testBatch() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			base.startBatch();
			base.setMsg("Changed");
			base.setMsg("Changed again");
			base.setMsg("Changed yet again");
			base.setMsg("Keep on changing");
			base.store();
			base.toString();
			base.debug("This is a debug message");
			base.info("This is an info message");
			base.warn(new Exception("Oops"), "This is a warning message");
			base.error(new Exception("Really bad"), "This is an error message");
			assertTrue(base.getVersion() == 2);
			List<LogEntry> logs = base.getLogs();
			for(LogEntry logentry : logs) {
				System.out.println(logentry);
			}
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}		
	}

	@Test
	public void testNothingChanged() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base.getVersion() == 1);
			base.setMsg("Changed");
			assertTrue(base.getVersion() == 2);
			base.store();
			// no effect
			assertTrue(base.getVersion() == 2);
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}		
	}

	@Test
	public void testUndoRedo() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			
			// create version 1
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			
			// create version 2
			base.setMsg("Changed");
			
			// create version 3
			base.setMsg("Changed again");
			
			// create version 4
			base.setMsg("Changed yet again");
			
			// create version 5
			base.setMsg("Keep on changing");
			
			assertTrue(base.getVersion() == 5);
			
			base.undo();
			assertTrue(base.getVersion() == 4);
			assertEquals("Changed yet again", base.getMsg());

			base.undo();
			assertTrue(""+base.getVersion(), base.getVersion() == 3);
			assertEquals("Changed again", base.getMsg());

			base.undo();
			assertTrue(base.getVersion() == 2);
			assertEquals("Changed", base.getMsg());

			base.undo();
			assertTrue(base.getVersion() == 1);
			assertEquals("Hello", base.getMsg());

			base.undo();
			assertTrue(base.getVersion() == 1);
			assertEquals("Hello", base.getMsg());
			
			base.redo();
			assertTrue(base.getVersion() == 2);
			assertEquals("Changed", base.getMsg());
			
			base.redo();
			assertTrue(base.getVersion() == 3);
			assertEquals("Changed again", base.getMsg());

			base.redo();
			assertTrue(base.getVersion() == 4);
			assertEquals("Changed yet again", base.getMsg());

			base.redo();
			assertTrue(base.getVersion() == 5);
			assertEquals("Keep on changing", base.getMsg());
			
			// now it gets complicated
			base.undo();
			assertTrue(base.getVersion() == 4);
			assertEquals("Changed yet again", base.getMsg());
			base.undo();
			assertTrue(base.getVersion() == 3);
			assertEquals("Changed again", base.getMsg());
			base.undo();
			assertTrue(base.getVersion() == 2);
			assertEquals("Changed", base.getMsg());
			base.undo();
			assertTrue(base.getVersion() == 1);
			assertEquals("Hello", base.getMsg());
			base.redo();
			assertTrue(base.getVersion() == 2);
			assertEquals("Changed", base.getMsg());
			
			base.setMsg("New branch");
			assertTrue(base.getVersion() == 6);
			assertEquals("New branch", base.getMsg());

			base.undo();
			assertTrue(base.getVersion() == 2);
			assertEquals("Changed", base.getMsg());

			// we cannot redo
			assertTrue(!base.canRedo());
			
			List<Long> options = base.redoOptions();
			assertTrue(options.get(0) == 3);
			assertTrue(options.get(1) == 6);
			
			base.redo(options.get(0));
			assertTrue(base.getVersion() == 3);
			assertEquals("Changed again", base.getMsg());
			
			base.undo();
			base.redo(6);
			assertTrue(base.getVersion() == 6);
			assertEquals("New branch", base.getMsg());
			base.undo();
			base.undo();
			assertTrue(base.getVersion() == 1);
			assertEquals("Hello", base.getMsg());
			
			
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}
	}

	// app.getVersions
	@Test
	public void testGetVersions() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			base.setMsg("Changed");
			base.setMsg("Changed again");
			base.setMsg("Changed yet again");
			base.setMsg("Keep on changing");
			assertTrue(base.getVersion() == 5);
			List<BaseSub> versions = base.getVersions();
			assertEquals(5, versions.size());
			int i=1;
			for(BaseSub version : versions) {
				System.out.println(version);
				assertEquals(i++, version.getVersion());
				assertTrue(!version.isCurrent());
			}
			
			base.select(versions.get(2));
			assertTrue(""+base.getVersion(), base.getVersion() == 3);
			assertEquals("Changed again", base.getMsg());
			// select is not undoable!
			base.undo();
			assertTrue(base.getVersion() == 2);
			assertEquals("Changed", base.getMsg());
			
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}		
	}
	

	// app.getAllLogs
	@Test
	public void testGetAllLogs() {
		Application app = mkApp();
		Base logger = app.create();
		try {
			assertTrue(app.getDb() != null);
			BaseSub base = app.create("base", BaseSub.class);
			assertTrue(base != null);
			base.setMsg("Changed");
			base.setMsg("Changed again");
			base.setMsg("Changed yet again");
			base.setMsg("Keep on changing");
			
			List<LogEntry> logs = app.getAllLogs();
			for(LogEntry log : logs) {
				/* :) */
				app.log.log(log);
			}
			assertTrue(logs.size() > 0);
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			logger.error(t.toString(), t);
			fail(t.toString());
		}		
	}
	
	@Test
	public void testAutoDeclare() {
		Application app = mkApp();
		Base log = app.create();
		try {
			assertTrue(app.getDb() != null);
			BaseSub base = app.create();
			assertEquals("base", base.getName());
			assertEquals(BaseSub.class, base.getClazz());
		} catch(Exception e) {
			Throwable t = WrapperException.unwrap(e);
			log.error(t.toString(), t);
			fail(t.toString());
		}		
	}

}
