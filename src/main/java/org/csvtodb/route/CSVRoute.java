package org.csvtodb.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;
import org.apache.camel.component.jackson.JacksonDataFormat;

import java.util.*;

@Component
public class CSVRoute extends RouteBuilder {
    CsvDataFormat csvDataFormat = new CsvDataFormat()
            .setDelimiter(',');

    JacksonDataFormat jsonDataFormat = new JacksonDataFormat();

    @Override
    public void configure() throws Exception {
        from("file:out")
                .unmarshal(csvDataFormat)
                .process(exchange -> {
                    // Get the CSV data
                    @SuppressWarnings("unchecked")
                    List<List<String>> csvData = (List<List<String>>) exchange.getIn().getBody();

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
                })
                .marshal(jsonDataFormat)
//                .to("http://10.2.5.51:3001/api/v1/lift/data");
                .to("file:data?fileName=output.json");
    }
}
