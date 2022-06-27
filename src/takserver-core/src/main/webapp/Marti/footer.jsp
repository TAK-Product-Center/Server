<%@ page language="java" contentType="text/html; charset=UTF-8"%>


<link rel=Stylesheet href="${pageContext.request.contextPath}/marti.css" type="text/css" media="screen" />
<div>
  <table>
    <tr>
	<p><center>
    <%="TAK Server " + ((com.bbn.marti.util.VersionBean) com.bbn.marti.util.spring.SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.util.VersionBean.class)).getVer()%> 
    </center></p> 
    <p><center>
    <i>Node ID: <%=((com.bbn.marti.util.VersionBean) com.bbn.marti.util.spring.SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.util.VersionBean.class)).getNodeId()%></i> 
    </center></p> 
    </div>
    </tr>
  </table>
</div>
