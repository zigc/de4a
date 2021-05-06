package eu.de4a.connector.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.w3c.dom.Document;

import eu.de4a.connector.api.ResponseApi;
import eu.de4a.connector.as4.owner.MessageResponseOwner;
import eu.de4a.connector.error.exceptions.ResponseErrorException;
import eu.de4a.connector.error.model.ExternalModuleError;
import eu.de4a.connector.error.model.LayerError;
import eu.de4a.iem.jaxb.common.types.ResponseErrorType;
import eu.de4a.iem.xml.de4a.DE4AMarshaller;
import eu.de4a.iem.xml.de4a.DE4AResponseDocumentHelper;
import eu.de4a.util.DE4AConstants;
import eu.de4a.util.DOMUtils;

@Controller
@Validated
public class ResponseController implements ResponseApi {
	private static final Logger logger = LoggerFactory.getLogger(ResponseController.class);

	@Autowired
	private ApplicationEventMulticaster applicationEventMulticaster;
	@Autowired
	private ApplicationContext context;

	@PostMapping(value = "/requestTransferEvidenceUSIDT", produces = MediaType.APPLICATION_XML_VALUE, 
            consumes = MediaType.APPLICATION_XML_VALUE)
	public String requestTransferEvidenceUSIDT(String request) {
		try {
			Document doc = DOMUtils.stringToDocument(request);
			String id = DOMUtils.getValueFromXpath(DE4AConstants.XPATH_ID, doc.getDocumentElement());

			MessageResponseOwner responseUSI = new MessageResponseOwner(context);
			responseUSI.setId(id);
			responseUSI.setMessage(doc.getDocumentElement());
			applicationEventMulticaster.multicastEvent(responseUSI);
		} catch (Exception e) {
		    String error = "There was an error processing RequestTransferEvidenceUSIDT";
			logger.error(error, e);
			throw new ResponseErrorException().withLayer(LayerError.INTERNAL_FAILURE)
			    .withModule(ExternalModuleError.CONNECTOR_DT)
			    .withMessageArg(error)
			    .withHttpStatus(HttpStatus.OK);
		}
		ResponseErrorType response = DE4AResponseDocumentHelper.createResponseError(true);
		return DE4AMarshaller.dtUsiResponseMarshaller().getAsString(response);
	}
}
