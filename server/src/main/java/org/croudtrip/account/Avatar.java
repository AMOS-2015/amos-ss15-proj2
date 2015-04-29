package org.croudtrip.account;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * An image which can be uploaded and stored in a DB.
 */
@Entity(name = Avatar.ENTITY_NAME)
@Table(name = "profile_images")
public class Avatar {

	public static final String
			ENTITY_NAME = "ProfileImage",
			COLUMN_ID = "profile_image_id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Column(name = "content", nullable = false)
	private byte[] content;

	@Column(name = "media_type", nullable = false)
	private String mediaType;

	public Avatar() { }

	public Avatar(byte[] content, String mediaType) {
		this.content = content;
		this.mediaType = mediaType;
	}


	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public byte[] getContent() {
		return content;
	}


	public void setContent(byte[] content) {
		this.content = content;
	}


	public String getMediaType() {
		return mediaType;
	}


	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

}
