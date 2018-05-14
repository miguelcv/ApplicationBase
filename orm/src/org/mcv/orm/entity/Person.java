package org.mcv.orm.entity;

import java.util.ArrayList;
import java.util.List;

import org.mcv.app.Base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
public class Person extends Base {

	@JsonCreator
	public Person(String name) {
		super(name);
	}

	public String getFirstName() {
		return getName().split("\\s")[0];
	}
	public String getLastName() {
		return getName().split("\\s")[1];
	}
	
	private IdCard idCard;
	private List<Phone> phones = new ArrayList<>();
}
