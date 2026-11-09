package com.jobcom.web;
import com.jobcom.dao.AdminDAO;
import com.jobcom.dao.JobPortalDAO;
import com.jobcom.dao.SupportDAO;
import com.jobcom.security.PasswordUtil;
import jakarta.servlet.*;
import java.util.logging.*;
public class AdminContextListener implements ServletContextListener {
 private static final Logger LOG=Logger.getLogger(AdminContextListener.class.getName());
 public void contextInitialized(ServletContextEvent e){
  try{new JobPortalDAO().ensureFeatureSchema();LOG.info("Job applications, compensation, chat, and career-field matching schema checked.");}
  catch(Exception ex){LOG.log(Level.SEVERE,"Job applications/chat schema upgrade failed; apply database/migrations/20261021_job_applications_chat.sql.",ex);}
  try{new SupportDAO().ensureSchema();LOG.info("Seeker and recruiter support conversation schema checked.");}
  catch(Exception ex){LOG.log(Level.SEVERE,"Support schema upgrade failed; apply database/migrations/20261029_recruiter_admin_support.sql.",ex);}
  String email=System.getenv("JOBCOM_ADMIN_EMAIL"),password=System.getenv("JOBCOM_ADMIN_PASSWORD");
  if(email==null||password==null||password.length()<12){LOG.warning("Admin bootstrap skipped: configure JOBCOM_ADMIN_EMAIL and a 12+ character JOBCOM_ADMIN_PASSWORD.");return;}
  try{new AdminDAO().bootstrap(email,PasswordUtil.hash(password));LOG.info("Admin account bootstrap checked.");}
  catch(Exception ex){LOG.log(Level.SEVERE,"Admin bootstrap failed; verify schema and JOBCOM_DB_* values.",ex);}
 }
}
