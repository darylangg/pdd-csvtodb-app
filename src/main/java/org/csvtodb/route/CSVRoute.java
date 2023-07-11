package org.csvtodb.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class CSVRoute extends RouteBuilder {
    CsvDataFormat csvDataFormat = new CsvDataFormat()
            .setDelimiter(',');

    JacksonDataFormat jsonDataFormat = new JacksonDataFormat();

    @Value("${csv.readDirectory}")
    private String readDirectory;

    @Value("${csv.completedDirectory}")
    private String completedDirectory;

    @Value("${csv.errorDirectory}")
    private String errorDirectory;

    @Value("${csv.cronInterval}")
    private String cronInterval;

    @Value("${hapi.url}")
    private String hapiUrl;

    @Override
    @SuppressWarnings("unchecked")
    public void configure() throws Exception {
        String fileReadURI = "file:"
                .concat(readDirectory)
                .concat("?recursive=true")
                .concat("&delete=true")
                .concat("&scheduler=quartz")
                .concat("&scheduler.cron=" + cronInterval);

        String fileOutURI = "file:"
                .concat(completedDirectory);

        String fileErrorURI = "file:"
                .concat(errorDirectory);

        from(fileReadURI)
            .routeId("CSV Scheduler Route")
                .log("Processing ${headers.CamelFileName}")
            .process(exchange->{
                exchange.setProperty("data", exchange.getIn().getBody());
                exchange.setProperty("csvHeaders", exchange.getIn().getHeaders());
            })
            .to("direct:sendToDB");

        from("direct:sendToDB")
            .routeId("DB Route")
            .doTry()
                .unmarshal(csvDataFormat)
                .process(exchange -> {
                    // Get the CSV data
                    List<List<String>> csvData = (List<List<String>>) exchange.getIn().getBody();

                    String fileName = (String) exchange.getMessage().getHeader(Exchange.FILE_NAME);
                    Path filePath = Paths.get(fileName);
                    String vertical = filePath.getParent().toString();

                    // Convert each row to a JSON object
                    List<Map<String, Object>> jsonObjects = new ArrayList<>();
                    for (int j=1; j < csvData.size(); j++){
                        List<String> row = csvData.get(j);
                        Map<String, Object> jsonObject = new LinkedHashMap<>();
                        for (int i = 0; i < row.size(); i++) {
                            // Use the first row as keys for the JSON object
                            String key = csvData.get(0).get(i);
                            String value = row.get(i);
                            jsonObject.put(key, value);
                        }
                        jsonObjects.add(jsonObject);
                    }
                    // Set the JSON objects as the new body
                    exchange.getIn().setBody(jsonObjects);
                    exchange.getIn().removeHeaders("*");
                    exchange.getIn().setHeader("vertical", vertical);
                })
                .marshal(jsonDataFormat)
                .toD("http://"+ hapiUrl +"/api/v1/${headers.vertical}/data")
                .removeHeaders("*")
                .setProperty("status", constant("success"))
                .to("direct:fileMove")
            .doCatch(Exception.class)
                .log("Error occurred: ${exception.message}")
                .setProperty("status", constant("error"))
                .to("direct:fileMove");

        from("direct:fileMove")
            .routeId("File Move Route")
            .process(exchange->{
                exchange.getIn().setBody(exchange.getProperty("data"));
                exchange.getIn().setHeaders((Map<String, Object>) exchange.getProperty("csvHeaders"));
            })
            .log("${headers.CamelFileName} : ${exchangeProperty.status}")
            .choice()
            .when(simple("${exchangeProperty.status} == 'success'"))
                .to(fileOutURI)
            .when(simple("${exchangeProperty.status} == 'error'"))
                .to(fileErrorURI);
    }
}
