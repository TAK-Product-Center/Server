package tak.server;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import com.bbn.marti.remote.socket.SituationAwarenessMessage;
import com.bbn.marti.remote.socket.TakMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class TakMessageConversionTests {

	@Autowired
	ObjectMapper mapper;
	
	@Test
	public void serializeMessageTest() throws DocumentException, ParserConfigurationException, SAXException, IOException {
		
		TakMessage tm = new SituationAwarenessMessage();
		
		tm.getAddresses().add("bob");
		tm.getAddresses().add("andrew");

		String json = mapper.writeValueAsString(tm);
		TakMessage tm1 = mapper.readValue(json, TakMessage.class);
	}
}
