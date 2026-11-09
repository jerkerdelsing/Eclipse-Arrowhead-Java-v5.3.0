package eu.arrowhead.localcloudconfigurator.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A saved graphical design. List and map fields are JSON text in the native API shape.
@Entity
@Table(name = "configuration")
public class SavedConfiguration {

	public static final int JSON_LENGTH = 1048576;

	@Id
	@Column(length = 36)
	private String id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = JSON_LENGTH)
	private String systems;

	@Column(nullable = false, length = JSON_LENGTH)
	private String removals;

	@Column(nullable = false)
	private boolean reapply;

	@Column(nullable = false)
	private boolean smoke;

	@Column(nullable = false, length = JSON_LENGTH)
	private String layout;

	@Column(nullable = false)
	private long revision;

	@Column(nullable = false, length = 40)
	private String updatedAt;

	public SavedConfiguration() {
	}

	public SavedConfiguration(final String id, final String name, final String systems, final String removals, final boolean reapply, final boolean smoke, final String layout,
			final long revision, final String updatedAt) {
		this.id = id;
		update(name, systems, removals, reapply, smoke, layout, revision, updatedAt);
	}

	public final void update(final String name, final String systems, final String removals, final boolean reapply, final boolean smoke, final String layout, final long revision,
			final String updatedAt) {
		this.name = name;
		this.systems = systems;
		this.removals = removals;
		this.reapply = reapply;
		this.smoke = smoke;
		this.layout = layout;
		this.revision = revision;
		this.updatedAt = updatedAt;
	}

	public String getId() { return id; }
	public String getName() { return name; }
	public String getSystems() { return systems; }
	public String getRemovals() { return removals; }
	public boolean isReapply() { return reapply; }
	public boolean isSmoke() { return smoke; }
	public String getLayout() { return layout; }
	public long getRevision() { return revision; }
	public String getUpdatedAt() { return updatedAt; }
}
