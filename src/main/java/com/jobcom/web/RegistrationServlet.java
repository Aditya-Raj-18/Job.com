package com.jobcom.web;

import com.jobcom.config.Db;
import com.jobcom.security.PasswordUtil;
import com.jobcom.util.CareerFields;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.*;
import java.util.UUID;

@MultipartConfig(fileSizeThreshold=1024*1024, maxFileSize=5*1024*1024, maxRequestSize=18*1024*1024)
public class RegistrationServlet extends HttpServlet {
    private static final String CSRF_KEY = "registrationCsrf";

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = "seeker".equals(request.getParameter("type")) ? "seeker" : "recruiter";
        HttpSession session = request.getSession();
        String csrf = (String) session.getAttribute(CSRF_KEY);
        if (csrf == null) { csrf = UUID.randomUUID().toString(); session.setAttribute(CSRF_KEY, csrf); }
        request.setAttribute("type", type);
        request.setAttribute("careerFields", CareerFields.all());
        request.setAttribute("csrf", csrf);
        request.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || !safeEquals((String) session.getAttribute(CSRF_KEY), request.getParameter("csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token"); return;
        }
        String type = "recruiter".equals(request.getParameter("type")) ? "recruiter" : "seeker";
        String email = value(request, "email").toLowerCase(Locale.ROOT);
        String password = request.getParameter("password");
        String storedDocument = null;
        Path uploadedFile = null; Path uploadedResume = null; Path uploadedPhoto = null;
        try {
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address.");
            if (password == null || password.length() < 12) throw new IllegalArgumentException("Password must be at least 12 characters long.");
            Part proof = request.getPart("proof");
            if (proof == null || proof.getSize() == 0) throw new IllegalArgumentException("Upload the required verification document.");
            String subfolder = type.equals("seeker") ? "ids" : "licenses";
            uploadedFile = saveProof(proof, subfolder);
            storedDocument = subfolder + "/" + uploadedFile.getFileName();
            String resumePath="";
            Part resume=request.getPart("resume");
            if(type.equals("seeker")&&resume!=null&&resume.getSize()>0){uploadedResume=saveResume(resume);resumePath="resumes/"+uploadedResume.getFileName();}
            String profilePhotoPath="";
            if(type.equals("seeker")){
                uploadedPhoto=saveProfilePhoto(request.getPart("profilePhoto"));
                profilePhotoPath="profile-pictures/"+uploadedPhoto.getFileName();
            }

            try (Connection connection = Db.getConnection()) {
            if (type.equals("seeker")) insertSeeker(connection, request, email, PasswordUtil.hash(password), storedDocument,resumePath,profilePhotoPath);
                else insertRecruiter(connection, request, email, PasswordUtil.hash(password), storedDocument);
            }
            session.removeAttribute(CSRF_KEY);
            response.sendRedirect(request.getContextPath() + "/register?type=" + type + "&success=1");
        } catch (IllegalArgumentException e) {
            deleteQuietly(uploadedFile);deleteQuietly(uploadedResume);deleteQuietly(uploadedPhoto);
            showError(request, response, type, e.getMessage());
        } catch (Exception e) {
            deleteQuietly(uploadedFile);deleteQuietly(uploadedResume);deleteQuietly(uploadedPhoto);
            getServletContext().log("Registration could not be saved", e);
            String message = isDuplicateKey(e)
                ? "An account with these details already exists. Please sign in or use a different email or phone number."
                : "Registration could not be saved. Please check your details and try again.";
            showError(request, response, type, message);
        }
    }

    private void insertSeeker(Connection c, HttpServletRequest r, String email, String hash, String document,String resumePath,String profilePhotoPath) throws SQLException {
        String fullName=required(r,"full_name","Full name"),idType=required(r,"government_id_type","Government ID type");
        String phone=required(r,"phone","Phone number");
        long phoneDigits=phone.chars().filter(Character::isDigit).count();
        if(phoneDigits<7||phoneDigits>15)throw new IllegalArgumentException("Enter a valid phone number with 7 to 15 digits.");
        String last4=required(r,"government_id_last4","Last four ID digits");
        if(!last4.matches("[0-9]{4}"))throw new IllegalArgumentException("Enter exactly the last four digits of the ID.");
        String careerField=value(r,"career_field");
        if(!CareerFields.isValid(careerField))throw new IllegalArgumentException("Choose your career field to see relevant job openings.");
        String dob=value(r,"date_of_birth"),experience=value(r,"experience_years");
        LinkedHashMap<String,Object> fields=new LinkedHashMap<>();Set<String> cols=columns(c,"users");
        if(!cols.contains("profile_photo_path")&&!cols.contains("profile_picture_path")&&!cols.contains("profile_picture")&&!cols.contains("avatar_path"))throw new SQLException("The user profile photo column is not available.");
        putFirst(cols,fields,fullName,"full_name","name");putFirst(cols,fields,email,"email");putFirst(cols,fields,hash,"password_hash");putFirst(cols,fields,phone,"phone");
        putFirst(cols,fields,dob.isBlank()?null:java.sql.Date.valueOf(dob),"date_of_birth");putFirst(cols,fields,value(r,"address"),"address");putFirst(cols,fields,value(r,"city"),"city");putFirst(cols,fields,value(r,"state"),"state");putFirst(cols,fields,value(r,"postal_code"),"postal_code");putAllPresent(cols,fields,profilePhotoPath,"profile_photo_path","profile_picture_path","profile_picture","avatar_path");
        putFirst(cols,fields,value(r,"qualification"),"qualification");putFirst(cols,fields,careerField,"career_field");putFirst(cols,fields,experience.isBlank()?null:new java.math.BigDecimal(experience),"experience_years");putFirst(cols,fields,value(r,"skills"),"skills");putFirst(cols,fields,value(r,"profile_headline"),"profile_headline");putFirst(cols,fields,value(r,"profile_summary"),"profile_summary");putFirst(cols,fields,value(r,"linkedin_url"),"linkedin_url");putFirst(cols,fields,value(r,"portfolio_url"),"portfolio_url");putFirst(cols,fields,resumePath,"resume_path");
        putFirst(cols,fields,idType,"government_id_type");putFirst(cols,fields,last4,"government_id_last4");putAllPresent(cols,fields,document,"government_id_document_path","verification_document_path");putFirst(cols,fields,"PENDING","id_verification_status");putFirst(cols,fields,"PENDING","status");
        insert(c,"users",fields);
    }

    private void insertRecruiter(Connection c, HttpServletRequest r, String email, String hash, String document) throws SQLException {
        String companyName=required(r,"company_name","Company name"),contact=required(r,"contact_person","Recruiter/contact name"),phone=required(r,"phone","Phone number");
        String license=required(r,"license_number","Company license number"),licenseType=required(r,"license_type","License type"),address=required(r,"address","Company address"),city=required(r,"city","City");
        LinkedHashMap<String,Object> fields=new LinkedHashMap<>();Set<String> cols=columns(c,"companies");
        putFirst(cols,fields,companyName,"name","company_name");putFirst(cols,fields,email,"email");putFirst(cols,fields,hash,"password_hash");putFirst(cols,fields,value(r,"legal_name"),"legal_name");putFirst(cols,fields,contact,"contact_person");putFirst(cols,fields,value(r,"contact_title"),"contact_title");putAllPresent(cols,fields,phone,"phone","office_phone");putFirst(cols,fields,value(r,"website"),"website");putFirst(cols,fields,value(r,"industry"),"industry");putFirst(cols,fields,value(r,"company_size"),"company_size");
        putFirst(cols,fields,license,"license_number");putFirst(cols,fields,licenseType,"license_type");putFirst(cols,fields,document,"license_document_path");putFirst(cols,fields,value(r,"company_registration_number"),"company_registration_number");putFirst(cols,fields,value(r,"tax_registration_number"),"tax_registration_number");putFirst(cols,fields,address,"address");putFirst(cols,fields,city,"city");putFirst(cols,fields,value(r,"state"),"state");putFirst(cols,fields,value(r,"country"),"country");putFirst(cols,fields,value(r,"postal_code"),"postal_code");putFirst(cols,fields,"PENDING","verification_status");putFirst(cols,fields,"PENDING","status");
        insert(c,"companies",fields);
    }

    private Set<String> columns(Connection c,String table)throws SQLException{
        Set<String> names=new HashSet<>();try(var result=c.getMetaData().getColumns(c.getCatalog(),null,table,null)){while(result.next())names.add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));}return names;
    }
    private void putFirst(Set<String> columns,Map<String,Object> fields,Object value,String... candidates){
        for(String candidate:candidates)if(columns.contains(candidate.toLowerCase(Locale.ROOT))){fields.put(candidate,value);return;}
    }
    private void putAllPresent(Set<String> columns,Map<String,Object> fields,Object value,String... candidates){
        for(String candidate:candidates)if(columns.contains(candidate.toLowerCase(Locale.ROOT)))fields.put(candidate,value);
    }
    private void insert(Connection c,String table,LinkedHashMap<String,Object> fields)throws SQLException{
        if(fields.isEmpty())throw new SQLException("No compatible columns found for "+table);
        String names=String.join(",",fields.keySet());String placeholders=String.join(",",Collections.nCopies(fields.size(),"?"));
        try(PreparedStatement p=c.prepareStatement("INSERT INTO `"+table+"` ("+names+") VALUES ("+placeholders+")")){int i=1;for(Object value:fields.values())p.setObject(i++,value);p.executeUpdate();}
    }

    private boolean isDuplicateKey(Throwable error){
        for(Throwable current=error;current!=null;current=current.getCause()){
            if(current instanceof SQLException sql){
                for(SQLException item=sql;item!=null;item=item.getNextException())
                    if("23000".equals(item.getSQLState())&&item.getErrorCode()==1062)return true;
            }
        }
        return false;
    }

    private Path saveProof(Part part, String subfolder) throws IOException {
        if (part.getSize() > 5L*1024*1024) throw new IllegalArgumentException("Verification document must be 5 MB or smaller.");
        String submitted=part.getSubmittedFileName();
        String ext=submitted==null?"":submitted.substring(submitted.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        if (!ext.matches("pdf|png|jpe?g")) throw new IllegalArgumentException("Upload a PDF, JPG, or PNG verification document.");
        byte[] signature=new byte[8]; int read;
        try(var in=part.getInputStream()){read=in.read(signature);}
        boolean valid=ext.equals("pdf")&&read>=4&&signature[0]=='%'&&signature[1]=='P'&&signature[2]=='D'&&signature[3]=='F'
            ||ext.equals("png")&&read>=8&&(signature[0]&255)==137&&signature[1]=='P'&&signature[2]=='N'&&signature[3]=='G'
            ||(ext.equals("jpg")||ext.equals("jpeg"))&&read>=3&&(signature[0]&255)==255&&(signature[1]&255)==216&&(signature[2]&255)==255;
        if(!valid)throw new IllegalArgumentException("The uploaded file does not match its PDF/JPG/PNG type.");
        Path folder=uploadRoot().resolve(subfolder).normalize();
        if(!folder.startsWith(uploadRoot()))throw new IOException("Invalid upload location");
        Files.createDirectories(folder);
        Path file=folder.resolve(UUID.randomUUID()+"."+ext).normalize();
        try(var in=part.getInputStream();var out=Files.newOutputStream(file)){in.transferTo(out);}
        return file;
    }

    private Path saveResume(Part part)throws IOException{
        if(part.getSize()>5L*1024*1024)throw new IllegalArgumentException("Resume must be 5 MB or smaller.");
        String filename=part.getSubmittedFileName();String ext=filename==null?"":filename.substring(filename.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        byte[] bytes;try(var in=part.getInputStream()){bytes=in.readAllBytes();}
        com.jobcom.util.AtsScore.extract(bytes,ext);
        Path root=uploadRoot();Path folder=root.resolve("resumes").normalize();if(!folder.startsWith(root))throw new IOException("Invalid upload location");Files.createDirectories(folder);Path file=folder.resolve(UUID.randomUUID()+"."+ext).normalize();Files.write(file,bytes);return file;
    }

    private Path saveProfilePhoto(Part part)throws IOException{
        if(part==null||part.getSize()==0)throw new IllegalArgumentException("Upload a profile photo to create your job seeker account.");
        if(part.getSize()>5L*1024*1024)throw new IllegalArgumentException("Profile photos must be 5 MB or smaller.");
        String submitted=part.getSubmittedFileName();String ext=submitted==null||submitted.lastIndexOf('.')<0?"":submitted.substring(submitted.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        if(!Set.of("jpg","jpeg","png").contains(ext))throw new IllegalArgumentException("Upload a JPG or PNG profile photo.");
        byte[] signature=new byte[8];int read;try(var in=part.getInputStream()){read=in.read(signature);}
        boolean png=ext.equals("png")&&read>=8&&(signature[0]&255)==137&&signature[1]=='P'&&signature[2]=='N'&&signature[3]=='G'&&signature[4]==13&&signature[5]==10&&signature[6]==26&&signature[7]==10;
        boolean jpeg=(ext.equals("jpg")||ext.equals("jpeg"))&&read>=3&&(signature[0]&255)==255&&(signature[1]&255)==216&&(signature[2]&255)==255;
        if(!png&&!jpeg)throw new IllegalArgumentException("The selected file is not a valid JPG or PNG image.");
        Path root=uploadRoot();Path folder=root.resolve("profile-pictures").normalize();if(!folder.startsWith(root))throw new IOException("Invalid profile photo upload directory.");Files.createDirectories(folder);
        Path target=folder.resolve(UUID.randomUUID()+(png?".png":".jpg")).normalize();if(!target.startsWith(root))throw new IOException("Invalid profile photo upload path.");
        try(var in=part.getInputStream()){Files.copy(in,target);}return target;
    }

    private Path uploadRoot() throws IOException {
        String configured=System.getenv("JOBCOM_UPLOAD_DIR");
        String realPath=getServletContext().getRealPath("/uploads");
        Path root=configured==null||configured.isBlank()?(realPath==null?Paths.get(System.getProperty("user.home"),"Job.com","uploads"):Paths.get(realPath)):Paths.get(configured);
        return root.toAbsolutePath().normalize();
    }
    private String value(HttpServletRequest r,String name){String v=r.getParameter(name);return v==null?"":v.trim();}
    private String required(HttpServletRequest r,String name,String label){String v=value(r,name);if(v.isBlank())throw new IllegalArgumentException(label+" is required.");return v;}
    private boolean safeEquals(String a,String b){return a!=null&&b!=null&&java.security.MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),b.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private void deleteQuietly(Path file){if(file!=null)try{Files.deleteIfExists(file);}catch(IOException ignored){}}
    private void showError(HttpServletRequest r,HttpServletResponse p,String type,String message)throws ServletException,IOException{
        String csrf=(String)r.getSession().getAttribute(CSRF_KEY);if(csrf==null){csrf=UUID.randomUUID().toString();r.getSession().setAttribute(CSRF_KEY,csrf);}
        r.setAttribute("type",type);r.setAttribute("careerFields",CareerFields.all());r.setAttribute("csrf",csrf);r.setAttribute("error",message);r.getRequestDispatcher("/WEB-INF/views/auth/register.jsp").forward(r,p);
    }
}
