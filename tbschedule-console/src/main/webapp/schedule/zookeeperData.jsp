<%@page contentType="text/html; charset=utf-8" %>
<%@page import="java.io.StringWriter"%>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%
if(ConsoleManager.isInitial() == false){
		response.sendRedirect("config.jsp");
}
%>
<%@include file="header.jsp"%>
<%
  String path = request.getParameter("path");
  if(path == null){
	  path = ConsoleManager.getScheduleStrategyManager().getRootPath();
  }
  StringWriter writer = new StringWriter();
  ConsoleManager.getScheduleStrategyManager().printTree(
		  path,writer,"<br/>");
%>
<pre>
<%=writer.getBuffer().toString()%>
</pre>
<%@include file="footer.jsp"%>