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
@EqualsAndHashCode(callSuper=false)
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
public class Project extends Base {

	@JsonCreator
	public Project(String name) {
		super(name);
	}

	public enum ProjectType {
		FIXED, TIME_AND_MATERIAL
	}

	private List<Geek> geeks = new ArrayList<>();
	private ProjectType projectType;
	private Period projectPeriod;
	private List<Period> billingPeriods = new ArrayList<>();
}
