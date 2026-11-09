package com.jobcom.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class AdminDocumentServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{
        String relative=request.getParameter("path");
        if(relative==null||relative.isBlank()||relative.indexOf('\\')>=0||relative.startsWith("/")||relative.contains("..")){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid document reference");return;
        }
        if(!relative.matches("(ids|licenses|resumes|resume-templates)/[a-fA-F0-9-]{36}\\.(pdf|png|jpe?g)")){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Unsupported document reference");return;
        }
        String configured=System.getenv("JOBCOM_UPLOAD_DIR");String realPath=getServletContext().getRealPath("/uploads");
        Path root=(configured==null||configured.isBlank())?(realPath==null?Paths.get(System.getProperty("user.home"),"Job.com","uploads"):Paths.get(realPath)):Paths.get(configured);
        root=root.toAbsolutePath().normalize();Path file=root.resolve(relative).normalize();
        if(!file.startsWith(root)||!Files.isRegularFile(file)){response.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        String name=file.getFileName().toString().toLowerCase(Locale.ROOT);String mime=name.endsWith(".pdf")?"application/pdf":name.endsWith(".png")?"image/png":"image/jpeg";
        response.setContentType(mime);response.setContentLengthLong(Files.size(file));response.setHeader("Content-Disposition","attachment; filename=\""+file.getFileName()+"\"");
        response.setHeader("X-Content-Type-Options","nosniff");response.setHeader("Cache-Control","private, no-store, max-age=0");
        try(var in=Files.newInputStream(file);var out=response.getOutputStream()){in.transferTo(out);}
    }
}
