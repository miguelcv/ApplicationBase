package org.mcv.orm.entity;

import java.util.Date;

import lombok.Data;

@Data
public class Period {
	private final Date startDate;
	private final Date endDate;
}
