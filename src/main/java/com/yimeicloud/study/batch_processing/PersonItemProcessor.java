package com.yimeicloud.study.batch_processing;

import java.text.DateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class PersonItemProcessor implements ItemProcessor<Person, Person> {
	private static final Logger logger = LoggerFactory.getLogger(PersonItemProcessor.class);

	@Override
	public Person process(Person person) throws Exception {
		
		//String firstName = " ";
		//String lastName = " ";
		
		String firstName = null;
		String lastName = null;
		if(null != person.getFirstName() && !"".equals(person.getFirstName())) {
			firstName = person.getFirstName();
		}
		if(null != person.getLastName() && !"".equals(person.getLastName())) {
			lastName = person.getLastName();
		}
		Person transformedPerson = new Person(firstName, lastName, DateFormat.getDateInstance().format(new java.util.Date()));
		logger.info("convert " + person + " to " + transformedPerson);
		return transformedPerson;
	}
}
