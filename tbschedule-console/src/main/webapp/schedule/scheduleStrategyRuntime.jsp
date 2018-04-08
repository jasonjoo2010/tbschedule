<%@page contentType="text/html; charset=utf-8" %>
<%@page import="java.util.ArrayList"%>
<%@page import="com.taobao.pamirs.schedule.strategy.ScheduleStrategyRunntime"%>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="java.util.List"%>
<%
 String strategyName =request.getParameter("strategyName");
 String uuid =request.getParameter("uuid");
%>
<%@include file="header.jsp"%>
<table border="1" >
     <tr>
     	<th>序号</th>
     	<th>任务类型</th>
     	<th>处理机器</th>
    	<th>线程组数量</th>
    	<th>错误信息</th>
     </tr>
<%
List<ScheduleStrategyRunntime> runntimeList = null;
if(strategyName != null && strategyName.trim().length() > 0){
	runntimeList =ConsoleManager.getScheduleStrategyManager().loadAllScheduleStrategyRunntimeByTaskType(strategyName);
}else if(uuid != null && uuid.trim().length() > 0){
	runntimeList =ConsoleManager.getScheduleStrategyManager().loadAllScheduleStrategyRunntimeByUUID(uuid);
}else{
	runntimeList =new ArrayList<ScheduleStrategyRunntime>();
}

for(int i=0;i<runntimeList.size();i++){
	ScheduleStrategyRunntime run = runntimeList.get(i);
%>
     <tr >
     	<td><%=(i+1)%></td>
     	<td><%=run.getStrategyName()%></td>
     	<td align="center"><%=run.getUuid()%></td>
     	<td align="center"><%=run.getRequestNum()%></td>
     	<td align="center" style="" ><p style="color:red"><%=run.getMessage()%></p></td>     	
     </tr>
<%
}
%>
</table>
<%@include file="footer.jsp"%>