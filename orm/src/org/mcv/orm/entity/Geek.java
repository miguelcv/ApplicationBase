package org.mcv.orm.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
@ToString(exclude = {"projects"})
public class Geek extends Person {

	@JsonCreator
	public Geek(String name) {
		super(name);
	}

	private String favouriteProgrammingLanguage;
	private List<Project> projects = new ArrayList<>();
}
