package eu.toop.req.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="evaluator_request")
public class EvaluatorRequest {
	@Id
	private String idrequest;
	private String urlreturn;
	private String idevaluator; 
	private boolean done;
	public String getIdrequest() {
		return idrequest;
	}
	public void setIdrequest(String idrequest) {
		this.idrequest = idrequest;
	}
	public String getUrlreturn() {
		return urlreturn;
	}
	public void setUrlreturn(String urlreturn) {
		this.urlreturn = urlreturn;
	}
	public String getIdevaluator() {
		return idevaluator;
	}
	public void setIdevaluator(String idevaluator) {
		this.idevaluator = idevaluator;
	}
	public boolean isDone() {
		return done;
	}
	public void setDone(boolean done) {
		this.done = done;
	}
	 
	
}
