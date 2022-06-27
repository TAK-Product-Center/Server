

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.dao.kml.JDBCCachingKMLDao;
import com.bbn.marti.sync.Metadata.Field;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;

// used by EnterpriseSync.jsp
public class FileList {
	static final int KBYTES = 1024;
	static final int MBYTES = 1048576;
	static final int GBYTES = 1073741824;

	private static final String DOWNLOAD = "sync/content?hash=";

	private static final Logger logger = LoggerFactory.getLogger(FileList.class);

	@Autowired
	private Validator validator;

	@Autowired
	private EnterpriseSyncService persistenceStore;

	@Autowired
	private JDBCCachingKMLDao dao;

	public FileList() { }

	public void writeFilesHtml(Writer out, HttpServletRequest request) {

		String groupVector = null;

		try {
			// Get group vector for the user associated with this session
			groupVector = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class).getGroupBitVector(request);
			logger.trace("groups bit vector: " + groupVector);
		} catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e.getMessage());
		}

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}


		try {
			Map<String, List<Metadata>> files = persistenceStore.getAllMetadata(groupVector);

			logger.debug("file list for enterpriseSync.jsp: " + files);


			out.write("<table border=\"1\">\n");
			out.write("<tr>\n");
			out.write("<th>Select</th>\n");
			out.write("<th>Name</th>\n");
			out.write("<th>Submitter (TLS cert)</th>\n");
			out.write("<th>Creator (self reported)</th>\n");
			//out.write("<th>UID</td>");
			out.write("<th>Keywords</th>\n");
			out.write("<th>Size (Approx)</th>\n");
			out.write("<th>Update Time</th>\n");
			out.write("<th>Type</th>\n");
			out.write("<th>Expiration</th>");
			out.write("<th>Actions</th>\n");
			out.write("</tr>\n");

			for(List<Metadata> list : files.values()) {
				for (Metadata metadata : list) {

					try {
						if (validator != null) {
							metadata.validate(this.validator);
						}
					} catch (ValidationException | IntrusionException ex) {
						StringBuilder builder = new StringBuilder();
						builder.append("Unsafe item from Enterprise Sync datbase: ");
						builder.append("Primary key " + metadata.getPrimaryKey() + " ");
						logger.warn(builder.toString());
						continue;
					}

					out.write("<tr>\n");

					// Select file
					out.write("<td align=center>");
					out.write("<input type=\"checkbox\" name=\"fileSelect\" value=\""
							+ metadata.getPrimaryKey() + "\" autocomplete=\"off\"/>");
					out.write("</td>\n");

					// Name
					out.write("<td><a href=\"" + DOWNLOAD + metadata.getHash() +"\">"
							+ metadata.getFirstSafely(Field.Name) + "</a>\n");
					out.write("</td>\n");

					// Upload user
					out.write("<td>");
					out.write(metadata.getFirstSafely(Metadata.Field.SubmissionUser));
					out.write("</td>\n");

					// Optional creator
					String uid = metadata.getFirstSafely(Field.CreatorUid);
					out.write("<td>");
					out.write(ESAPI.encoder().encodeForHTML(dao.latestCallsign(uid)));
					out.write("</td>\n");


					String[] keywords = metadata.getKeywords();
					if (keywords == null) {
					    out.write("<td></td>\n");
					} else {
					    out.write("<td>");
					    for (int i = 0; i < keywords.length; i++) {
					        out.write(keywords[i]);
					        if (i != keywords.length - 1) {
					            out.write(", ");
					        }
					    }
					    out.write("</td>\n");
					}

					// UID
					//out.write("<td>");
					//out.write(metadata.getUid());
					//out.write("</td\n>");

					// Size
					out.write("<td>");
					Integer size = metadata.getSize();
					if (size == null ) {
						out.write("Unknown");
					} else {
						if(size < 1024) {
							out.write(size + "B");
                        } else if(size < MBYTES) {
							out.write((size/KBYTES) + "kB");
                        } else if(size < GBYTES) {
							out.write(size/MBYTES + "MB");
                        } else {
							out.write(size/GBYTES + "GB");
					}
					}
					out.write("</td>\n");

					// Update time
					out.write("<td>");
					out.write(metadata.getFirstSafely(Metadata.Field.SubmissionDateTime) + "\n");
					out.write("</td>\n");

					// MIME type
					out.write("<td>");
					out.write(metadata.getFirstSafely(Metadata.Field.MIMEType));
					out.write("</td>\n");

					// Expiration
					Long expiration = metadata.getExpiration();
					String expiration_string = "";
					if (expiration != null && expiration >= 0) {
					    expiration_string = Instant.ofEpochSecond(expiration).toString();
					    expiration_string = expiration_string.substring(0, expiration_string.length() - 1);
					} else {
					    expiration_string = "none";
					}
					out.write("<td>");
					out.write("<input type=\"datetime-local\" value=" + expiration_string + " class='expirationVal' id='" + metadata.getPrimaryKey() + "'>");
					out.write("<input type=\"button\" value=\"Clear\" class='clearExpiration' id='" + metadata.getPrimaryKey() + "'>");
					out.write("<input type=\"button\" value=\"Set\" class='setExpiration' id='" + metadata.getPrimaryKey() + "'>");
					out.write("<input type='hidden' value='" + metadata.getHash() + "' class='hashExpiration' name='" + metadata.getPrimaryKey() + "' style=\"display:none\">");
					out.write("</td>\n");

					// Actions
					out.write("<td><button class='delfile' id='" + metadata.getPrimaryKey() + "' "
							+ "name='" + metadata.getFirstSafely(Metadata.Field.Name) + "' "
							+ "/>Delete</button> </td>");

					// End of row
					out.write("</tr>\n");
				}
			}
			out.write("</table>\n");
		} catch (IOException ex) {
			logger.error("Failed to write to output stream: " + ex.getMessage());
		} catch (ParseException | ValidationException ex) {
			String message = "<h3> <Font COLOR=\"DC143C\"> Invalid data from PostGreSQL Database. </Font> </h3>\n"
					+ "<p>Results from database query could not be parsed. Contact the Marti administrator.</p>\n";
			logger.error(ex.getMessage());
			try {
				out.write(message);
			} catch (IOException ioex) {
				logger.error("Failed to write error message to Web client: " + ex.getMessage() + "\n Original message was: \n" + message);
			}
		} catch (NamingException | SQLException e) {
			String message = "<h3> <Font COLOR=\"DC143C\"> Error communicating with PostGreSQL Database. </Font> </h3>\n"
					+ "<p>Make sure that the database server is running, then refresh this page </p>\n";
			logger.error(e.getMessage());
			try {
				out.write(message);
			} catch (IOException ex) {
				logger.error("Failed to write error message to Web client: " + ex.getMessage() + "\n Original message was: \n" + message);
			}
		}
	}
}
