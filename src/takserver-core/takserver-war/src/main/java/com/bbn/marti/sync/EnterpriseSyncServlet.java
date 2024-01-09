

package com.bbn.marti.sync;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.EsapiServlet;
import com.bbn.marti.util.CommonUtil;

/**
 * Abstract base class for Enterprise Sync servlets that need to talk to the enterprise sync database.
 * This base class includes a PersistenceStore instance that is instantiated with the proper
 * validation enforcement bit derived from the runtime environment.
 * 
 *
 */
public abstract class EnterpriseSyncServlet extends EsapiServlet {
	private static final long serialVersionUID = 1449343475322942936L;
	
	@Autowired
	protected EnterpriseSyncService enterpriseSyncService;
	
	@Autowired
	protected CommonUtil commonUtil;
	
	@Override
    public void init(final ServletConfig config) throws ServletException {
		super.init(config);
	}
	
	@Override
	protected abstract void initalizeEsapiServlet();

}
