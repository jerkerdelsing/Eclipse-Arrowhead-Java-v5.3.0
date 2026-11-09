package eu.arrowhead.localcloudconfigurator.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A submitted deployment: the reviewed plan and request, and the last state and step results reported by the executing backend.
@Entity
@Table(name = "deployment")
public class Deployment {

	@Id
	@Column(length = 128)
	private String id;

	@Column(nullable = false, length = SavedConfiguration.JSON_LENGTH)
	private String plan;

	@Column(nullable = false, length = SavedConfiguration.JSON_LENGTH)
	private String request;

	@Column(nullable = false, length = 32)
	private String state;

	@Column(nullable = true, length = SavedConfiguration.JSON_LENGTH)
	private String result;

	@Column(nullable = false, length = 40)
	private String submittedAt;

	@Column(nullable = false, length = 40)
	private String updatedAt;

	public Deployment() {
	}

	public Deployment(final String id, final String plan, final String request, final String state, final String submittedAt) {
		this.id = id;
		this.plan = plan;
		this.request = request;
		this.state = state;
		this.submittedAt = submittedAt;
		this.updatedAt = submittedAt;
	}

	public void observe(final String state, final String result, final String at) {
		this.state = state;
		this.result = result;
		this.updatedAt = at;
	}

	public String getId() { return id; }
	public String getPlan() { return plan; }
	public String getRequest() { return request; }
	public String getState() { return state; }
	public String getResult() { return result; }
	public String getSubmittedAt() { return submittedAt; }
	public String getUpdatedAt() { return updatedAt; }
}
