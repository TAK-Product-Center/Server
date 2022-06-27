<%@ page language="java" contentType="application/json" pageEncoding="UTF-8" %>
<%@ page import="java.rmi.Naming" %>
<%@ page import="com.bbn.marti.remote.GeoCacheInterface" %>
<%
GeoCacheInterface geocache = (GeoCacheInterface) Naming.lookup("//127.0.0.1:3334/GeoCache");
%>
{ "backhaulBytesSaved" : <%= geocache.getBackhaulBytesSaved() %> }
