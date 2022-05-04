package eu.de4a.connector.api.controller;

import java.io.InputStream;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import eu.de4a.connector.api.NotificationAPI;
import eu.de4a.connector.api.manager.APIManager;
import eu.de4a.connector.config.DE4AConstants;
import eu.de4a.connector.dto.AS4MessageDTO;
import eu.de4a.connector.error.exceptions.ConnectorException;
import eu.de4a.connector.error.handler.ConnectorExceptionHandler;
import eu.de4a.connector.error.model.EExternalModuleError;
import eu.de4a.connector.utils.APIRestUtils;
import eu.de4a.iem.core.DE4ACoreMarshaller;
import eu.de4a.iem.core.jaxb.common.EventNotificationType;

@Controller
@RequestMapping("/event")
public class EventController implements NotificationAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private APIManager apiManager;

    @PostMapping(value = "/notification/", produces = MediaType.APPLICATION_XML_VALUE,
            consumes = MediaType.APPLICATION_XML_VALUE)
    @Override
    public ResponseEntity<byte[]> eventNotification(@Valid final InputStream request) {
        LOGGER.debug("Request to API /notification/ received");

        final var marshaller = DE4ACoreMarshaller.dtEventNotificationMarshaller();

        final EventNotificationType eventObj = (EventNotificationType) APIRestUtils
                .conversionBytesWithCatching(marshaller, request, false, true,
                        new ConnectorException().withModule(EExternalModuleError.CONNECTOR_DT));

        // Check if there are multiple evidence responses
        final String docTypeID;
        if(eventObj.getEventNotificationItemCount() > 1) {
            docTypeID = DE4AConstants.EVENT_CATALOGUE_SCHEME + "::" +
                    DE4AConstants.MULTI_ITEM_TYPE;
        } else {
            docTypeID = eventObj.getEventNotificationItemAtIndex(0)
                    .getCanonicalEventCatalogUri();
        }

        final AS4MessageDTO messageDTO = new AS4MessageDTO(eventObj.getDataOwner().getAgentUrn(),
                eventObj.getDataEvaluator().getAgentUrn())
                    .withContentID(eventObj.getClass().getSimpleName())
                    .withDocTypeID(docTypeID)
                    .withProcessID(DE4AConstants.MESSAGE_TYPE_NOTIFICATION);

        final boolean isSent = this.apiManager.processIncomingMessage(eventObj, messageDTO, eventObj.getNotificationId(),
                "Event Notification", marshaller);

        return ResponseEntity.status((isSent ? HttpStatus.OK: HttpStatus.INTERNAL_SERVER_ERROR))
                .body((byte[]) ConnectorExceptionHandler.getResponseError(null, isSent));
    }

}
