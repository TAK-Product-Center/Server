

package com.bbn.marti;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.util.KmlUtils;

public class KmlMasterSaServlet extends EsapiServlet
{

  private static final long serialVersionUID = -6732080432220291977L;
  public static final String DEFAULT_FILENAME = "TAK-Master-Links.kml";
  @Override
  protected void initalizeEsapiServlet()
  {
    log = Logger.getLogger(KmlMasterSaServlet.class.getCanonicalName());
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) 
      throws ServletException, IOException {
    
    initAuditLog(request);
    
    int secAgoInt = 0;
    String secagoArg = request.getParameter("secago");
    if (secagoArg != null) {
      try {
        secAgoInt = (int) Double.parseDouble(secagoArg);
      } catch (NumberFormatException ex) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal value for parameter \"secago\"" );
      }
    }

    String baseurl = KmlUtils.getBaseUrl(request);
    
    response.setContentType("application/vnd.google-earth.kml+xml");
    response.setHeader("Content-Disposition", "filename=" + DEFAULT_FILENAME);
    
    PrintWriter writer = response.getWriter();
    writer.println("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>");
    writer.println("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");
    writer.println("  <Folder>");
    writer.println("    <open>1</open>");
    writeSubsection(writer, "Friendlies", "a-f", secAgoInt, true, baseurl);
    writeSubsection(writer, "Neutrals", "a-n", secAgoInt, false, baseurl);
    writeSubsection(writer, "Unknowns", "a-u", secAgoInt, false, baseurl);
    writeSubsection(writer, "Hostiles", "a-h", secAgoInt, false, baseurl);
    writeSubsection(writer, "Routes", "b-m-r", secAgoInt, false, baseurl);
    writer.println("  </Folder>");
    writer.println("</kml>");
  }
  
  protected void writeSubsection(PrintWriter writer, String name, String cotType, 
    int secAgo, boolean visible, String baseurl) {
    
    int refreshInterval = 60;
    if (secAgo > 0) {
      refreshInterval = secAgo;
    }
    
    writer.println("    <NetworkLink>");
    writer.println("      <name>"+name+"</name>");
    writer.println("      <open>0</open>");
    writer.println("      <visibility>"+(visible?1:0)+"</visibility>");
    writer.println("      <description>Latest location of "+name+" objects</description>");
    writer.println("        <Link>");
    writer.println("          <href>" + baseurl + "/LatestKML?cotType="+cotType+"&amp;secago="+secAgo+"</href>");
    writer.println("          <refreshInterval>"+refreshInterval+"</refreshInterval>");
    writer.println("          <refreshMode>onInterval</refreshMode>");
    writer.println("        </Link>");
    writer.println("    </NetworkLink>");
  }  
}
