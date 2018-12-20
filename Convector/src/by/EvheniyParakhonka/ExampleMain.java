package by.EvheniyParakhonka;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExampleMain {


    private static final int PRETTY_PRINT_INDENT_FACTOR = 4;

    public static void main(String... args) throws IOException {
//       Json to xml
        String jsonStr = "{\"Customer\": {" +
                "\"address\": {" +
                "\"street\": \"NANTERRE CT\"," +
                "\"postcode\": 77471" +
                "}," +
                "\"name\": \"Mary\"," +
                "\"age\": 37" +
                "}}";

        JSONObject json = new JSONObject(jsonStr);
        String xml = XML.toString(json);

        System.out.println(xml);


        // 2. Convert Json File -> XML File
        String jsonFile = System.getProperty("user.dir") + "\\generated.json";
        String xmlFile = System.getProperty("user.dir") + "\\generated.xml";

        jsonStr = new String(Files.readAllBytes(Paths.get(jsonFile)));
        JSONObject jsonArray = new JSONObject(jsonStr);

        try (FileWriter fileWriter = new FileWriter(xmlFile)){
            fileWriter.write(XML.toString(jsonArray));
            System.out.println(XML.toString(jsonArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
//      XML to JSON
        String xmlString = "<?xml version=\"1.0\"?>\n" +
                "<Company>\n" +
                "  <Employee>\n" +
                "      <FirstName>Tanmay</FirstName>\n" +
                "      <LastName>Patil</LastName>\n" +
                "      <ContactNo>1234567890</ContactNo>\n" +
                "      <Email>tanmaypatil@xyz.com</Email>\n" +
                "      <Address>\n" +
                "           <City>Bangalore</City>\n" +
                "           <State>Karnataka</State>\n" +
                "           <Zip>560212</Zip>\n" +
                "      </Address>\n" +
                "  </Employee>\n" +
                "</Company>\n";

        JSONObject xmlJSONObj = XML.toJSONObject(xmlString);
        String jsonPrettyPrintString = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);

        System.out.println(jsonPrettyPrintString);

        // 2. Convert XML File -> Json File
        String xmlFile2 = System.getProperty("user.dir") + "\\generated.xml";

        xmlString = new String(Files.readAllBytes(Paths.get(xmlFile2)));
        xmlJSONObj = XML.toJSONObject(xmlString);

        String jsonFile2 = System.getProperty("user.dir") + "\\file.json";

        try (FileWriter fileWriter = new FileWriter(jsonFile2)){
            fileWriter.write(xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR));
            System.out.println(xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR));
        }
    }
}
