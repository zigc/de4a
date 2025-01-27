package eu.de4a.connector.dto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.w3c.dom.Document;
import com.helger.commons.annotation.Nonempty;

/**
 * AS4 message data representation for DE4AConnector
 *
 */
@NotThreadSafe
public final class AS4MessageDTO {
  private final String senderID;
  private final String receiverID;
  private final String docTypeID;
  private final String processID;
  private Document message;

  public AS4MessageDTO(@Nonnull @Nonempty final String sSenderID, @Nonnull @Nonempty final String sReceiverID,
      @Nonnull @Nonempty final String sDocTypeID, @Nonnull @Nonempty final String sProcessID) {
    senderID = sSenderID;
    receiverID = sReceiverID;
    this.docTypeID = sDocTypeID;
    this.processID = sProcessID;
  }

  @Nonnull
  @Nonempty
  public String getSenderID() {
    return senderID;
  }

  @Nonnull
  @Nonempty
  public String getReceiverID() {
    return receiverID;
  }

  @Nonnull
  @Nonempty
  public String getDocTypeID() {
    return docTypeID;
  }

  @Nonnull
  @Nonempty
  public String getProcessID() {
    return processID;
  }

  @Nullable
  public Document getMessage() {
    return message;
  }

  public AS4MessageDTO withMessage(@Nullable final Document message) {
    this.message = message;
    return this;
  }
}
