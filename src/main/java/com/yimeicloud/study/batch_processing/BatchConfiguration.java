package com.yimeicloud.study.batch_processing;

import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.oxm.xstream.XStreamMarshaller;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private DataSource dataSource;
	
	@Bean
	public XStreamMarshaller marshaller() {
		final HashMap<String, String> aliases = new HashMap<String, String>();
		aliases.put("person", "com.yimeicloud.study.batch_processing.Person");
		
		return new XStreamMarshaller(){{
			setAliases(aliases);
		}};
	}
	
	// tag::readerwriterprocessor[]
	@Bean
	public FlatFileItemReader<Person> csvReader() {
		FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
		reader.setResource(new PathResource("D:\\eclipse_ws\\tmp\\sample-data.csv"));
		reader.setLineMapper(new DefaultLineMapper<Person>(){{
			setLineTokenizer(new DelimitedLineTokenizer(){{
				setNames(new String[] {"firstName", "lastName"});
			}});
			setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>(){{
				setTargetType(Person.class);
			}});
		}});
		
		return reader;
	}
	
	@Bean
	public StaxEventItemReader<Person> xmlReader() {
		StaxEventItemReader<Person> reader = new StaxEventItemReader<Person>();
		reader.setResource(new PathResource("D:\\eclipse_ws\\tmp\\sample-data.xml"));
		reader.setUnmarshaller(marshaller());
		reader.setFragmentRootElementName("person");
		
		return reader;
	}
	
	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}
	
	@Bean
	public JdbcBatchItemWriter<Person> dbWriter() {
		JdbcBatchItemWriter<Person> writer = new JdbcBatchItemWriter<Person>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Person>());
		writer.setSql("INSERT INTO people(first_name, last_name) VALUES(:firstName, :lastName)");
		writer.setDataSource(dataSource);
		
		return writer;
	}
	
	@Bean
	public FlatFileItemWriter<Person> csvWriter() {
		FlatFileItemWriter<Person> writer = new FlatFileItemWriter<Person>();
		writer.setResource(new PathResource("D:\\eclipse_ws\\tmp\\output.csv"));
		writer.setLineAggregator(new DelimitedLineAggregator<Person>(){{
			setDelimiter(",");
			setFieldExtractor(new BeanWrapperFieldExtractor<Person>(){{
				setNames(new String[] {"firstName", "lastName"});
			}});
		}});
		return writer;
	}
	
	@Bean
	public StaxEventItemWriter<Person> xmlWriter() {
		StaxEventItemWriter<Person> writer = new StaxEventItemWriter<Person>();
		writer.setResource(new PathResource("D:\\eclipse_ws\\tmp\\output.xml"));
		writer.setMarshaller(marshaller());
		writer.setRootTagName("persons");
		return writer;
	}
	// end::readerwriterprocessor[]
	
	// tag::listener[]
	@Bean
	public JobExecutionListener listener() {
		return new JobCompletionNotificationListener(new JdbcTemplate(dataSource));
	}
	// end::listener[]
	
	// tag::jobstep[]
	@Bean
	public Step csvStep() {
		return stepBuilderFactory.get("csvStep")
				.<Person, Person>chunk(10)
				.reader(csvReader())
				.processor(processor())
				.writer(csvWriter())
				//.writer(dbWriter())
				.build();
	}
	
	@Bean
	public Step xmlStep() {
		return stepBuilderFactory.get("xmlStepCsv")
				.<Person, Person>chunk(10)
				.reader(xmlReader())
				.processor(processor())
				//.writer(csvWriter())
				.writer(xmlWriter())
				.build();
	}
	
	@Bean
	public Job importUserJob() {
		return jobBuilderFactory.get("importUserJob")
				.incrementer(new RunIdIncrementer())
				.listener(listener())
				.flow(xmlStep())
				.end()
				.build();
	}
	// end::jobstep[]
}
