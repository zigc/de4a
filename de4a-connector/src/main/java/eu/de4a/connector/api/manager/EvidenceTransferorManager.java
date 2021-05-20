package eu.de4a.connector.api.manager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.SimpleIdentifierFactory;

import eu.de4a.connector.as4.client.regrep.RegRepTransformer;
import eu.de4a.connector.as4.owner.MessageOwner;
import eu.de4a.connector.as4.owner.MessageResponseOwner;
import eu.de4a.connector.as4.owner.OwnerLocator;
import eu.de4a.connector.client.Client;
import eu.de4a.connector.error.exceptions.ConnectorException;
import eu.de4a.connector.error.exceptions.OwnerException;
import eu.de4a.connector.error.exceptions.ResponseErrorException;
import eu.de4a.connector.error.exceptions.ResponseTransferEvidenceException;
import eu.de4a.connector.error.model.ExternalModuleError;
import eu.de4a.connector.error.model.FamilyErrorType;
import eu.de4a.connector.error.model.LayerError;
import eu.de4a.connector.error.model.MessageKeys;
import eu.de4a.connector.error.utils.ErrorHandlerUtils;
import eu.de4a.connector.error.utils.ResponseErrorFactory;
import eu.de4a.connector.model.OwnerAddresses;
import eu.de4a.connector.model.RequestorRequest;
import eu.de4a.connector.model.smp.NodeInfo;
import eu.de4a.connector.repository.RequestorRequestRepository;
import eu.de4a.connector.service.spring.MessageUtils;
import eu.de4a.exception.MessageException;
import eu.de4a.iem.jaxb.common.types.RequestTransferEvidenceUSIIMDRType;
import eu.de4a.iem.jaxb.common.types.ResponseTransferEvidenceType;
import eu.de4a.iem.xml.de4a.DE4AMarshaller;
import eu.de4a.iem.xml.de4a.IDE4ACanonicalEvidenceType;
import eu.de4a.kafkaclient.DE4AKafkaClient;
import eu.de4a.util.DE4AConstants;
import eu.de4a.util.DOMUtils;
import eu.toop.connector.api.me.outgoing.MEOutgoingException;
import eu.toop.connector.api.rest.TCPayload;

@Component
public class EvidenceTransferorManager extends EvidenceManager {
    private static final Logger logger = LoggerFactory.getLogger (EvidenceTransferorManager.class);

    @Autowired
    private Client client;
    @Autowired
    private OwnerLocator ownerLocator;
    @Autowired
    private RequestorRequestRepository requestorRequestRepository;


    public void queueMessage(MessageOwner request) {
        logger.info("Queued message to be send to the owner - RequestId: {}, DataEvaluatorId: {}, DataOwnerId: {}",
                request.getId(), request.getSenderId(), request.getReceiverId());
        if (logger.isDebugEnabled()) {
            logger.debug(DOMUtils.documentToString(request.getMessage().getOwnerDocument()));
        }
        ResponseTransferEvidenceType responseTransferEvidenceType = null;
        RequestTransferEvidenceUSIIMDRType req = null;
        ConnectorException ex = new OwnerException().withModule(ExternalModuleError.CONNECTOR_DT)
                .withHttpStatus(HttpStatus.OK);
        try {
            OwnerAddresses ownerAddress = getOwnerAddress(request.getReceiverId(), ex);
            RequestorRequest requestorReq = new RequestorRequest();
            if(!DE4AConstants.NAMESPACE_USI.equals(request.getMessage().getNamespaceURI())) {
                req = (RequestTransferEvidenceUSIIMDRType) ErrorHandlerUtils
                        .conversionDocWithCatching(DE4AMarshaller.drImRequestMarshaller(),
                                request.getMessage().getOwnerDocument(), false, false,
                                new ResponseTransferEvidenceException().withModule(ExternalModuleError.CONNECTOR_DT));
                
                requestorReq.setCanonicalEvidenceTypeId(req.getCanonicalEvidenceTypeId());
                requestorReq.setDataOwnerId(req.getDataOwner().getAgentUrn());
                responseTransferEvidenceType = (ResponseTransferEvidenceType) client.sendEvidenceRequest(req,
                        ownerAddress.getEndpoint(), false);
                if (responseTransferEvidenceType != null) {
                    Document docResponse = (Document) ErrorHandlerUtils.conversionDocWithCatching(
                            DE4AMarshaller.drImResponseMarshaller(IDE4ACanonicalEvidenceType.NONE),
                            responseTransferEvidenceType, true, false,
                            new ResponseTransferEvidenceException().withModule(ExternalModuleError.CONNECTOR_DT));
                    // TODO if as4 message DT-DR failed, what is the approach. retries?
                    if (!sendResponseMessage(req.getDataEvaluator().getAgentUrn(),req.getDataOwner().getAgentUrn(),
                            req.getCanonicalEvidenceTypeId(), docResponse.getDocumentElement(), DE4AConstants.TAG_EVIDENCE_RESPONSE)) {
                        String errorMsg = MessageFormat.format("Error sending ResponseTransferEvidence to Data Requestor via AS4 gateway - "
                                + "RequestId: {0}", req.getRequestId());
                        DE4AKafkaClient.send(EErrorLevel.ERROR, errorMsg);
                    }
                } else {
                    throw ex.withFamily(FamilyErrorType.CONNECTION_ERROR).withModule(ExternalModuleError.DATA_OWNER)
                            .withMessageArg("Response from owner was empty");
                }
            } else {
                req = (RequestTransferEvidenceUSIIMDRType) ErrorHandlerUtils.conversionDocWithCatching(
                        DE4AMarshaller.drUsiRequestMarshaller(), request.getMessage().getOwnerDocument(), false, 
                        true, new ResponseErrorException().withModule(ExternalModuleError.CONNECTOR_DT));
                requestorReq.setCanonicalEvidenceTypeId(req.getCanonicalEvidenceTypeId());
                requestorReq.setDataOwnerId(req.getDataOwner().getAgentUrn());
                client.sendEvidenceRequest(req, ownerAddress.getEndpoint(), true);
            }
            // Save request information
            requestorReq.setIdrequest(request.getId());
            requestorReq.setEvidenceServiceUri(request.getReceiverId());
            requestorReq.setSenderId(request.getSenderId());
            requestorReq.setDone(false);
            requestorRequestRepository.save(requestorReq);

        } catch (ConnectorException e) {
            responseTransferEvidenceType = (ResponseTransferEvidenceType) ResponseErrorFactory
                    .getHandlerFromClassException(ex.getClass()).buildResponse(ex);

            if (req == null || !sendResponseMessage(request.getSenderId(), request.getReceiverId(),
                    req.getCanonicalEvidenceTypeId(), DE4AMarshaller.drImResponseMarshaller(IDE4ACanonicalEvidenceType.NONE)
                            .getAsDocument(responseTransferEvidenceType).getDocumentElement(),
                    DE4AConstants.TAG_EVIDENCE_RESPONSE)) {
                logger.error("Error sending ResponseTransferEvidence to Data Requestor via AS4 gateway");
            }
        }
    }

    public void queueMessageResponse(MessageResponseOwner response) {
        logger.info("Queued response from owner USI pattern - RequestId: {}, DataEvaluatorId: {}, DataOwnerId: {}", 
                response.getId(), response.getDataEvaluatorId(), response.getDataOwnerId());
        if (logger.isDebugEnabled()) {          
            logger.debug(DOMUtils.documentToString(response.getMessage().getOwnerDocument()));
        }
        ConnectorException ex = new ResponseErrorException()
                .withLayer(LayerError.INTERNAL_FAILURE)
                .withModule(ExternalModuleError.CONNECTOR_DT);
        RequestorRequest usirequest = requestorRequestRepository.findById(response.getId()).orElse(null);
        if (usirequest == null) {
            String msgError = MessageFormat.format("The corresponding request to the received response is not found on database - "
                    + "RequestId: {0}", response.getId());
            DE4AKafkaClient.send(EErrorLevel.WARN, "Processing RequestTransferEvidenceUSIDT - " + msgError);
            throw ex.withFamily(FamilyErrorType.SAVING_DATA_ERROR).withMessageArg(msgError);
        } else {
            if(!sendResponseMessage(usirequest.getSenderId(), usirequest.getDataOwnerId(), usirequest.getCanonicalEvidenceTypeId(), 
                    response.getMessage(), DE4AConstants.TAG_EVIDENCE_REQUEST_DT)) {
                String errorMsg = MessageFormat.format("Error sending message to Data Requestor via AS4 gateway - "
                        + "RequestId: {0}", response.getId());                
                DE4AKafkaClient.send(EErrorLevel.ERROR, errorMsg);
                
                throw ex.withFamily(FamilyErrorType.AS4_ERROR_COMMUNICATION)
                    .withModule(ExternalModuleError.CONNECTOR_DR).withMessageArg(errorMsg);
            }
        }
    }

    public boolean sendResponseMessage(String receiverId, String dataOwnerId, String docTypeID, Element message, String tagContentId) {
        String errorMsg;
        try {
            IParticipantIdentifier doPI = SimpleIdentifierFactory.INSTANCE
                    .parseParticipantIdentifier(dataOwnerId.toLowerCase(Locale.ROOT));
            IParticipantIdentifier rPI = SimpleIdentifierFactory.INSTANCE
                    .parseParticipantIdentifier(receiverId.toLowerCase(Locale.ROOT));
            if(doPI != null) {
                dataOwnerId = doPI.getValue();
            }
            if(rPI != null) {
                receiverId = rPI.getValue();
            }
            
            NodeInfo nodeInfo = client.getNodeInfo(receiverId, docTypeID, true, message);
            DE4AKafkaClient.send(EErrorLevel.INFO, MessageFormat.format("Sending response message via AS4 gateway - "
                    + "DataEvaluatorId: {0}, Message tag: {1}, CanonicalEvidenceType: {2}", 
                    receiverId, tagContentId, docTypeID));
            
            List<TCPayload> payloads = new ArrayList<>();
            TCPayload payload = new TCPayload();
            payload.setContentID(tagContentId);
            payload.setValue(DOMUtils.documentToByte(message.getOwnerDocument()));
            payload.setMimeType(MediaType.APPLICATION_XML_VALUE);
            payloads.add(payload);
            Element requestWrapper = new RegRepTransformer().wrapMessage(message, false);
            
            as4Client.sendMessage(dataOwnerId, nodeInfo, requestWrapper, payloads, false);
            
            return true;
        } catch (NullPointerException | MEOutgoingException e) {
            errorMsg = "Error with AS4 gateway comunications: " + e.getMessage();
            
        } catch (MessageException e) {
            errorMsg = "Error building wrapper message: " + e.getMessage();
        }
        if(logger.isDebugEnabled())
            logger.debug(errorMsg);

        return false;
    }
    
    private OwnerAddresses getOwnerAddress(String dataOwnerId, ConnectorException ex) {
        DE4AKafkaClient.send(EErrorLevel.INFO, MessageFormat.format("Looking for Data Owner address - "
                + "DataOwnerId: {0}", dataOwnerId));
        
        OwnerAddresses ownerAddress = ownerLocator.lookupOwnerAddress(dataOwnerId);
        if (ownerAddress == null) {
            DE4AKafkaClient.send(EErrorLevel.ERROR,
                    MessageFormat.format("Data Owner address not found - DataOwnerId: {0}", dataOwnerId));

            throw ex.withFamily(FamilyErrorType.SAVING_DATA_ERROR).withLayer(LayerError.CONFIGURATION)
                    .withMessageArg(new MessageUtils(MessageKeys.ERROR_OWNER_NOT_FOUND, new Object[] { dataOwnerId }));
        }
        return ownerAddress;
    }

}
