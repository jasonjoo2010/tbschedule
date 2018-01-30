package com.taobao.pamirs.schedule.console;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.taobao.pamirs.schedule.ConsoleManager;


public class WebInitial extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void init() throws ServletException {
		super.init();
		try {
			ConsoleManager.initial();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
