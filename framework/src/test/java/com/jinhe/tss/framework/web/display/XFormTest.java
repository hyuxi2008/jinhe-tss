package com.jinhe.tss.framework.web.display;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jinhe.tss.framework.component.log.Log;
import com.jinhe.tss.framework.component.log.LogAction;
import com.jinhe.tss.framework.web.dispaly.XmlPrintWriter;
import com.jinhe.tss.framework.web.dispaly.xform.XFormDecoder;
import com.jinhe.tss.framework.web.dispaly.xform.XFormEncoder;

public class XFormTest {
	
	@Test
	public void testXForm() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		Log entity = new Log();
		entity.setId(12L);
		entity.setOperatorBrowser("Chrome 44");
		
		XFormEncoder encoder = new XFormEncoder(LogAction.LOG_XFORM_TEMPLET_PATH, entity);
		
		entity = (Log) XFormDecoder.decode(encoder.toXml(), Log.class);
		Assert.assertEquals("Chrome 44", entity.getOperatorBrowser());
		
		try {
			XmlPrintWriter writer = new XmlPrintWriter(response.getWriter());
			encoder.print(writer);
		} 
		catch (IOException e) {
			Assert.fail();
		}
	}
}
