package org.mcv.orm.entity;

import org.mcv.app.Base;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
@ToString(exclude = {"person"})
public class Phone extends Base {

	@JsonCreator
	public Phone(String name) {
		super(name);
	}
	
	public String getNumber() {
		return getName();
	}
	
	@JsonIgnore
	private Person person;
}
