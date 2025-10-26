package com.demo;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html><head><title>Sample App</title></head><body>");
        out.println("<h2>Sample Java Webapp - CI/CD Demo</h2>");
        out.println("<p>Build number: " + System.getenv("BUILD_NUMBER") + "</p>");
        out.println("<p><a href='/'>Home</a></p>");
        out.println("</body></html>");
    }
}
