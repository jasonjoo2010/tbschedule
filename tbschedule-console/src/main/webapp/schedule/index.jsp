<%@page contentType="text/html; charset=utf-8" %>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%
	try {
		ConsoleManager.initial();
		ConsoleManager.getScheduleDataManager();
	String isManager = request.getParameter("manager");
%>
<%@include file="header.jsp"%>
<h1 align="center">TaobaoSchedule调度管理控制台</h1>
<a id="strategyList" onclick="linkOnclick(this);"  target="content" href="scheduleStrategyList.jsp?manager=<%=isManager%>" style="color:#0000CD">调度策略</a>
<a id="taskTypeList" onclick="linkOnclick(this);" target="content" href="taskTypeList.jsp?manager=<%=isManager%>" style="color:#0000CD">任务管理</a>
<a id="managerFactoryList" onclick="linkOnclick(this);"  target="content" href="managerFactoryList.jsp?manager=<%=isManager%>" style="color:#0000CD">机器管理</a>
<a id="serverList" onclick="linkOnclick(this);"  target="content" href="serverList.jsp" style="color:#0000CD">处理线程组列表</a>

<%
	if ("true".equals(isManager)) {
%>
	<a id="config" onclick="linkOnclick(this);"  target="content" href="config.jsp" style="color:#0000CD">Zookeeper连接配置</a>
	<a id="zookeeperData" onclick="linkOnclick(this);"  target="content" href="zookeeperData.jsp" style="color:#0000CD">Zookeeper数据</a>
	<a id="zookeeperDataExport" onclick="linkOnclick(this);"  target="content" href="exportConfig.jsp" style="color:#0000CD">Export配置数据</a>
	<a id="zookeeperDataImport" onclick="linkOnclick(this);"  target="content" href="importConfig.jsp" style="color:#0000CD">Import配置数据</a>
<%
	}
%>
&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp请参考：<a href="http://code.taobao.org/p/tbschedule/wiki/index/">tbschedule开源项目</a>
&nbsp&nbsp<font style="color:red;font-size:15">[建议使用IE8]</font>
<iframe  name="content" FRAMEBORDER="0"  height="85%" width="100%" src="scheduleStrategyList.jsp?manager=<%=isManager%>"></iframe>

<script>
function linkOnclick(obj){
	taskTypeList.style.backgroundColor="";
	strategyList.style.backgroundColor="";
	managerFactoryList.style.backgroundColor="";
	serverList.style.backgroundColor="";
	config.style.backgroundColor="";
	zookeeperData.style.backgroundColor="";
	zookeeperDataExport.style.backgroundColor="";
	zookeeperDataImport.style.backgroundColor="";
	
	obj.style.backgroundColor="#FF0000";	
}

</script>
<%
	} catch (Exception e) {
		response.sendRedirect("config.jsp?error=" + e.getMessage());
	}
%>
<%@include file="footer.jsp"%>