package com.template.demo.launch;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ResourceBundle;

public class LaunchTomcat {

    private static ResourceBundle rb = ResourceBundle.getBundle("application");
    public static void startingTomcatToDisplayGreetings() {
        String port = System.getenv("port") !=null ? System.getenv("port") : rb.getString("port");
        String greetingMessage = rb.getString("server.greetingMessage");
        String title = rb.getString("server.title");
        String servletName = rb.getString("server.servletName");
        String servletContext = System.getenv("context");
        

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir("temp");
        tomcat.setPort(Integer.parseInt(port));
        tomcat.getConnector();
        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        Tomcat.addServlet(ctx, servletName, new HttpServlet() {
            protected void service(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                Writer w = resp.getWriter();
                w.write("<html>");
                w.write("<head>");
                w.write("<title>"+ title +"</title>");
                w.write("</head>");
                w.write("</html>");
                w.write(greetingMessage);
                w.flush();
            }
        });
        if(servletContext != null  && !servletContext.isEmpty()){
            ctx.addServletMappingDecoded(servletContext+"/*",servletName);
        }else {
            ctx.addServletMappingDecoded("/*",servletName);
        }
       

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            System.out.println("Exception occur:" + e.getMessage());
        }
        tomcat.getServer().await();
    }

}
