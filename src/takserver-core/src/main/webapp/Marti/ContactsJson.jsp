<%@ page language="java" contentType="application/json" pageEncoding="UTF-8" %>
<%@ page import="java.rmi.Naming" %>
<%@ page import="java.net.URI" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.bbn.marti.remote.ContactManager" %>
<%@ page import="com.bbn.marti.remote.RemoteContact" %>
{
		<%
	ContactManager contactMgr = (ContactManager) Naming
		.lookup("//127.0.0.1:3334/ContactManager");

		Iterator<RemoteContact> i = contactMgr.getContactList().iterator();
		while(i.hasNext()) {
	RemoteContact contact = i.next();
	if(contact != null && i.hasNext()) {
%>
		"<%= contact.getContactName() %>":<%= (System.currentTimeMillis() - contact.getLastHeardFromMillis()) %>,
		<%	
			} else if (contact != null) {
		%>
		"<%= contact.getContactName() %>":<%= (System.currentTimeMillis() - contact.getLastHeardFromMillis()) %>
		<%	
			}
		}
		%>
}
