package org.mcv.app;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class BaseSub extends Base {

	int i;
	long z;
	double d;
	String msg;
	Date date;

	public BaseSub(String name) {
		super(name);
		i = 10;
		z = 9;
		d = 9.9;
		msg = "Hello";
		date = new Date();
	}

}
