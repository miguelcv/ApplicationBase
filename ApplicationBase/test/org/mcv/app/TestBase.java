package org.mcv.app;

import org.junit.Test;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class TestBase {

	@Data @EqualsAndHashCode(callSuper=true)
	static class BaseSubSub extends BaseSub {

		int i;
		double d;
		String s;
		
		public BaseSubSub(String name) {
			super(name);
		}		
	}
	
	@Test
	public void test() {
		/*Application app = new Application("App");
		Base base = app.create();
		System.out.println(Base.getClasspath(base));
		BaseSub sub = app.create();
		System.out.println(Base.getClasspath(sub));
		BaseSubSub subSub = app.create();
		System.out.println(Base.getClasspath(subSub));*/
		Application app = new Application("app");
		Base base = new Base("base");
		BaseSub sub = new BaseSub("sub");
		BaseSubSub subSub = new BaseSubSub("subSub");
		
		System.out.println(base.getClass().getName());
		System.out.println(base.getClass().getSimpleName());
		System.out.println(base.getClass().getCanonicalName());
		System.out.println(base.getClass().getTypeName());
		System.out.println("");
		System.out.println(sub.getClass().getName());
		System.out.println(sub.getClass().getSimpleName());
		System.out.println(sub.getClass().getCanonicalName());
		System.out.println(sub.getClass().getTypeName());
		System.out.println("");		
		System.out.println(subSub.getClass().getName());
		System.out.println(subSub.getClass().getSimpleName());
		System.out.println(subSub.getClass().getCanonicalName());
		System.out.println(subSub.getClass().getTypeName());

		//Base xbase = app.create("xbase", Base.class);
		//BaseSub xsub = app.create("xsub", BaseSub.class);
		//BaseSubSub xsubSub = app.create("xsubSub", BaseSubSub.class);
		Base xbase = app.create();
		BaseSub xsub = app.create();
		BaseSubSub xsubSub = app.create();
		
		System.out.println("");
		System.out.println(xbase.getClass().getName());
		System.out.println(xbase.getClass().getSimpleName());
		System.out.println(xbase.getClass().getCanonicalName());
		System.out.println(xbase.getClass().getTypeName());
		System.out.println("");
		System.out.println(xsub.getClass().getName());
		System.out.println(xsub.getClass().getSimpleName());
		System.out.println(xsub.getClass().getCanonicalName());
		System.out.println(xsub.getClass().getTypeName());
		System.out.println("");
		System.out.println(xsubSub.getClass().getName());
		System.out.println(xsubSub.getClass().getSimpleName());
		System.out.println(xsubSub.getClass().getCanonicalName());
		System.out.println(xsubSub.getClass().getTypeName());
		System.out.println("");

		System.out.println(xbase.getClassName());
		System.out.println(xbase.getClassFullName());
		System.out.println(xbase.getClassPath());
		System.out.println("");
		System.out.println(xsub.getClassName());
		System.out.println(xsub.getClassFullName());
		System.out.println(xsub.getClassPath());
		System.out.println("");
		System.out.println(xsubSub.getClassName());
		System.out.println(xsubSub.getClassFullName());
		System.out.println(xsubSub.getClassPath());
	}

}
