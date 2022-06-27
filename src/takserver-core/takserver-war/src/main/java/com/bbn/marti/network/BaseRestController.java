package com.bbn.marti.network;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;


@RequestMapping("/Marti/api")
public class BaseRestController {

    public void setCacheHeaders(HttpServletResponse response) {

    	response.setHeader("Cache-Control", "must-revalidate, max-age=0, no-cache, no-store");
    	response.setDateHeader("Expires", 0);
    }
	
}
