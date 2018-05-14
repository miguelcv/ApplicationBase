package org.mcv.orm.entity;

import java.util.Date;

import org.mcv.app.Base;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class IdCard extends Base {
	
	@JsonCreator
	public IdCard(String name) {
		super(name);
	}

	public String getNumber() {
		return getName();
	}
	
	private Date issueDate;
	private boolean valid;
}
