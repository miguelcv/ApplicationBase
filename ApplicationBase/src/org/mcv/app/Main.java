package org.mcv.app;


public class Main {

	public static void main(String[] args) {
		Application app = new Application("main");
		
		try {
			BaseSub base = app.create();
			if(base.getName().equals("base") && base.getClazz().equals(BaseSub.class)) {
				System.out.println("Success!");
			} else {
				System.out.println("Failure");				
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
