package org.mcv.orm;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.mcv.app.Application;
import org.mcv.orm.entity.Geek;
import org.mcv.orm.entity.IdCard;
import org.mcv.orm.entity.Period;
import org.mcv.orm.entity.Person;
import org.mcv.orm.entity.Phone;
import org.mcv.orm.entity.Project;

public class Main extends Application {

	public Main(String name) {
		super(name);
	}

	public static void main(String[] args) {
		Main main = new Main("ORMTest");
		main.run();
	}

	public void run() {
		try {
			persistPerson();
			persistGeeks();
			loadPersons();
			addPhones();
			createProject();
			queryProject();
		} catch (Exception e) {
			this.getLog().error(e, "Error %s", e);
		}
	}

	private void persistPerson() {
		try {
			Person person = createAs("Homer Simpson");
			IdCard idCard = createAs("4711");
			idCard.setIssueDate(new Date());
			person.setIdCard(idCard);
			idCard.store();
			person.store();
		} catch (Exception e) {
			// person rollback!
			// idcard rollback!
			getLog().error(e, "Failed %s", e.toString());
		}
	}

	private void persistGeeks() {
		persistGeek("Gavin Coffee", "Java");
		persistGeek("Thomas Micro", "C#");
		persistGeek("Christian Cup", "Java");
	}
	
	private void persistGeek(String name, String lang) {
		Geek geek = createAs(name);
		geek.setFavouriteProgrammingLanguage(lang);
		geek.store();
	}

	private void loadPersons() {
		List<Person> resultList = getList(Person.class);
		for (Person person : resultList) {
			StringBuilder sb = new StringBuilder();
			sb.append(person.getName());
			if (person instanceof Geek) {
				Geek geek = (Geek)person;
				sb.append(" ").append(geek.getFavouriteProgrammingLanguage());
			}
			IdCard idCard = person.getIdCard();
			if (idCard != null) {
				sb.append(" ").append(idCard.getNumber()).append(" ").append(idCard.getIssueDate());
			}
			List<Phone> phones = person.getPhones();
			for (Phone phone : phones) {
				sb.append(" ").append(phone.getNumber());
			}
			getLog().info(sb.toString());
		}
	}

	private void addPhones() {
		List<Person> resultList = query(Person.class, p -> p.getFirstName().equals("Homer") && p.getLastName().equals("Simpson"));
		for (Person person : resultList) {
			Phone phone = createAs("+49 1234 456789");
			person.getPhones().add(phone);
			phone.setPerson(person);
			phone.store();
			person.store();
		}
	}

	private void createProject() {
		List<Geek> resultList = query(Geek.class, g -> g.getFavouriteProgrammingLanguage().equals("Java"));
		Project javaProject = create();
		javaProject.setProjectType(Project.ProjectType.TIME_AND_MATERIAL);
		Period period = new Period(createDate(1, 1, 2015), createDate(31, 12, 2015));
		javaProject.setProjectPeriod(period);
		for (Geek geek : resultList) {
			javaProject.getGeeks().add(geek);
			geek.getProjects().add(javaProject);
			geek.store();
		}
		javaProject.store();
	}

	private void queryProject() {
		List<Project> projects = query(Project.class, (Project t) -> t.getProjectPeriod().getStartDate().after(createDate(31, 12, 2014)));
		for (Project project : projects) {
			getLog().info(project.getProjectPeriod().getStartDate().toString());
		}
	}

	private Date createDate(int day, int month, int year) {
		GregorianCalendar gc = new GregorianCalendar();
		gc.set(Calendar.DAY_OF_MONTH, day);
		gc.set(Calendar.MONTH, month - 1);
		gc.set(Calendar.YEAR, year);
		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		gc.set(Calendar.SECOND, 0);
		gc.set(Calendar.MILLISECOND, 0);
		return new Date(gc.getTimeInMillis());
	}
}
