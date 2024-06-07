package cloudgene.mapred.api.v2.jobs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
// import java.util.Comparator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.io.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.CloudgeneJob;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.util.BaseResource;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.PublicUser;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlParameterInput;
import cloudgene.mapred.wdl.WdlParameterInputType;
import genepi.hadoop.HdfsUtil;
import genepi.hadoop.importer.ImporterFactory;
import genepi.io.FileUtil;

import java.util.List;
import java.util.ArrayList;

public class SubmitJob extends BaseResource {

	private static final Log log = LogFactory.getLog(SubmitJob.class);
	
	private static final String PARAM_JOB_NAME = "job-name";

	@Post
	public Representation post(Representation entity) {

		User user = getAuthUserAndAllowApiToken();

		if (getSettings().isMaintenance() && !user.isAdmin()) {
			return error503("This functionality is currently under maintenance.");
		}

		String appId = getAttribute("tool");
		try {
			appId = java.net.URLDecoder.decode(appId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e2) {
			return error404("Application '" + appId + "' is not in valid format.");
		}
		log.info("post: Application ID: "+appId);

		ApplicationRepository repository = getApplicationRepository();
		Application application = repository.getByIdAndUser(appId, user);
		WdlApp app = null;
		try {
			app = application.getWdlApp();
		} catch (Exception e1) {
			return error404("Application '" + appId + "' not found or the request requires user authentication.");
		}

		if (app.getWorkflow() == null) {
			return error404("Application '" + appId + "' has no mapred section.");
		}

		WorkflowEngine engine = getWorkflowEngine();
		Settings settings = getSettings();

		/* report some settings */
		log.info("\npost: cloudgene settings");
		log.info("post: Settings: getTempPath: "+settings.getTempPath());
		log.info("post: Settings: getHdfsWorkspace: "+settings.getHdfsWorkspace());
		log.info("post: Settings: getLocalWorkspace "+settings.getLocalWorkspace());
		log.info("post: Settings: getHdfsAppWorkspace: "+settings.getHdfsAppWorkspace());
		log.info("post: Settings: getName: "+settings.getName());
		for (String key: settings.getCluster().keySet()){
		    log.info("post: Settings: getCluster: "+key+" = "+settings.getCluster().get(key));
		}
		if (settings.getExternalWorkspace()!=null){
		    for (String key: settings.getExternalWorkspace().keySet()){
			log.info("post: Settings: getExternalWorkspace: "+key+" = "+settings.getExternalWorkspace().get(key));
		    }
		}
		log.info("");
		/* -- */

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
		String id = "job-" + sdf.format(new Date());

		boolean publicMode = false;
		if (user != null) {
			// private mode
			//int maxPerUser = settings.getMaxRunningJobsPerUser();
			int maxPerUser = user.getMaxRunningJobs();
			log.info("max jobs for user "+user.getUsername()+": "+maxPerUser);
			if (!user.isAdmin() && engine.getJobsByUser(user).size() >= maxPerUser) {
				return error400("Only " + maxPerUser + " jobs per user can be executed simultaneously.");
			}

		} else {
			// public mode
			user = PublicUser.getUser(getDatabase());
			String uuid = UUID.randomUUID().toString();
			id = id + "-" + HashUtil.getSha256(uuid);
			publicMode = true;
		}

		String hdfsWorkspace = "";
		try {
			hdfsWorkspace = HdfsUtil.path(getSettings().getHdfsWorkspace(), id);
		} catch (NoClassDefFoundError e) {
			log.warn("Hadoop not found in classpath. Ignore HDFS Workspace.");
		}
		log.info("HDFS Workspace: "+hdfsWorkspace);
		
		String localWorkspace = FileUtil.path(getSettings().getLocalWorkspace(), id);
		FileUtil.createDirectory(localWorkspace);
		log.info("local Workspace: "+localWorkspace);

		Map<String, String> inputParams = null;

		try {
			inputParams = parseAndUpdateInputParams_chunks(entity, app, hdfsWorkspace, localWorkspace);
		} catch (FileUploadIOException e) {
			return error400("Upload limit reached.");
		} catch (FileUploadException e) {
			return error400(e.getMessage());
		}

		if (inputParams == null) {
			return error400("Error during input parameter parsing.");
		}
		else{
		    for (String key: inputParams.keySet()){
			log.info("post: inputParams: "+key+" = "+inputParams.get(key));
			if (key.equals("files")){
			    File D=new File(inputParams.get(key));
			    File flist[] = D.listFiles();
			    log.info("List of files and directories in "+D);
			    for(File file : flist) {
				log.info("File name: "+file.getName());
			    }
			    log.info("");
			}
		    }
		}

		// if (inputParams.get("cur_chunk").equals("10")) {
		//     return error400("Test error");
		// }

		String jobid_report=inputParams.get("jobid");
		String hws_report=inputParams.get("hws");
		String lws_report=inputParams.get("lws");
		if (jobid_report.equals("NA")){
		    jobid_report=id;
		    lws_report=localWorkspace;
		    hws_report=hdfsWorkspace;
		}
		/* TODO: public mode / ID / new ID / name */
		String name = jobid_report;
		if (!publicMode) {
			if (inputParams.get(PARAM_JOB_NAME) != null && !inputParams.get(PARAM_JOB_NAME).trim().isEmpty()) {
				name = inputParams.get(PARAM_JOB_NAME);
			}
		}
		
		if (inputParams.get("cur_chunk").equals(inputParams.get("total_chunks"))){
		    log.info("Job name: "+name);
		    // merge all part files
		    mergeFileParts(inputParams.get("files"));
		    CloudgeneJob job = new CloudgeneJob(user, jobid_report, app, inputParams);
		    job.setId(jobid_report);
		    job.setName(name);
		    job.setLocalWorkspace(lws_report);
		    job.setHdfsWorkspace(hws_report);
		    job.setSettings(getSettings());
		    job.setRemoveHdfsWorkspace(getSettings().isRemoveHdfsWorkspace());
		    job.setApplication(app.getName() + " " + app.getVersion());
		    job.setApplicationId(appId);
		    String userAgent = getRequest().getClientInfo().getAgent();
		    job.setUserAgent(userAgent);
		    engine.submit(job);

		    Map<String, Object> params = new HashMap<String, Object>();
		    params.put("id",jobid_report);
		    String message = String.format("Job: Created job ID %s for user %s (ID %s - email %s)",jobid_report,user.getUsername(),user.getId(), user.getMail());
		    if (this.isAccessedByApi()) {
			message += " (via API token)";
		    }
		    log.info(message);
		    return ok("Your job was successfully added to the job queue.", params);
		}
		else{
		    Map<String, Object> params = new HashMap<String, Object>();
		    params.put("id",jobid_report);
		    params.put("hws",hws_report);
		    params.put("lws",lws_report);
		    return ok("Received chunk "+inputParams.get("cur_chunk")+" / "+inputParams.get("total_chunks"),params);
		}
	}

    private static String checksum(MessageDigest digest,File file) throws IOException{
        FileInputStream fis = new FileInputStream(file);
 
        // Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
 
        while ((bytesCount = fis.read(byteArray)) != -1){
	    digest.update(byteArray, 0, bytesCount);
	}
	fis.close();
 
	byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
       
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
	
        return sb.toString();
    }
    
    private boolean mergeFileList(List <String> files,String output){
	log.info("Merging "+files+" to "+output);
	try{
	    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(output))));
	    for (String fname: files.stream().sorted().collect(Collectors.toList())){
		log.info("File: "+fname);
		try{
		    byte fileBytes [] = FileUtils.readFileToByteArray(new File(fname));
		    out.write(fileBytes);
		    out.flush();
		}
		catch (IOException ex){
		    log.error(ex.toString());
		    return false;
		}
	    }
	    try{
		out.close();
		log.info("");
	    }
	    catch (IOException ex){
		log.error(ex.toString());
		return false;
	    }
	}
	catch (FileNotFoundException ex){
	    log.error(ex.toString());
	    return false;
	}
	return true;
    }
    
    private boolean mergeFileParts(String dir){
	File D=new File(dir);
	FileFilter fileFilter = new WildcardFileFilter("*.vcf.gz.part*");
	File flist [] = D.listFiles(fileFilter);
	log.info("Found "+flist.length+" files matching *.vcf.gz.part*");
	for(File file : flist) {
	    log.info("File name: "+file.getName());
	}
	log.info("");
	Pattern pattern = Pattern.compile("(.*)\\.vcf\\.gz\\.part\\d+$");
	Map<String, List<String>> M = new HashMap<String, List<String>>();
	for (File f:flist){
	    Matcher matcher = pattern.matcher(f.getName());
	    while (matcher.find()) {
		String p=matcher.group(1);
		if (!M.containsKey(p)) {
		    M.put(p, new ArrayList<String>());
		}
		M.get(p).add(f.getAbsolutePath());
		log.info(p+" : "+f.getName());
	    }
	}
	for (Map.Entry<String, List <String>> entry: M.entrySet()){
	    log.info(entry.getKey()+" -- "+entry.getValue());
	    mergeFileList(entry.getValue(),FileUtil.path(dir,entry.getKey()+".vcf.gz"));	    
	}
	// delete all *part* files
	for (File f: flist){
	    if (f.delete())
		log.info("Deleted "+f.getName());
	    else
		log.error("Deleting "+f.getName()+" failed");
	}
	// report MD5 sums
	// log.info("Saving MD5 checksums of the input files");
	// try{
	//     MessageDigest mdigest = MessageDigest.getInstance("MD5");
	//     fileFilter = new WildcardFileFilter("*.vcf.gz");
	//     flist = D.listFiles(fileFilter);
	//     log.info("Found "+flist.length+" files matching *.vcf.gz");
	//     for(File file : flist) {
	// 	try{
	// 	    log.info("checksum: "+file.getName()+" "+checksum(mdigest,file));
	// 	}catch (IOException ex) {
	// 	    log.error(ex.toString());
	// 	}
	// 	log.info("");
	//     }
	// }catch (NoSuchAlgorithmException ex) {
	//     log.error(ex.toString());
	//     return false;
	// }

	return true;
    }
    
	private Map<String, String> parseAndUpdateInputParams(Representation entity, WdlApp app, String hdfsWorkspace,
			String localWorkspace) throws FileUploadIOException, FileUploadException {
		Map<String, String> props = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();

		FileItemIterator iterator = null;
		try {
			iterator = parseRequest(entity);
		} catch (Exception e) {
			e.printStackTrace();
			return null;

		}
		// uploaded files
		try {
			while (iterator.hasNext()) {

				FileItemStream item = iterator.next();

				String entryName = StringEscapeUtils.escapeHtml(item.getName());

				if (entryName != null) {
				    log.info("parseAndUpdateInputParams: entryName: "+entryName);

					File file = null;

					try {
						// file parameter
						// write local file
						String tmpFile = getSettings().getTempFilename(entryName);
						log.info("parseAndUpdateInputParams: tmpFile: "+tmpFile);
						file = new File(tmpFile);

						// read file from stream
						FileUtils.copyInputStreamToFile(item.openStream(), file);

						// remove upload identification!
						String fieldName = item.getFieldName().replace("-upload", "").replace("input-", "");
						log.info("parseAndUpdateInputParams: fieldName: "+fieldName);

						// boolean hdfs = false;
						// boolean folder = false;

						WdlParameterInput inputParam = getInputParamByName(app, fieldName);

						if (inputParam == null) {
							throw new Exception("after calling getInputParamByName, parameter '" + fieldName + "' not found.");
						}

						if (inputParam.isHdfs()) {
							String targetPath = HdfsUtil.path(hdfsWorkspace, fieldName);
							String cleandEntryName = new File(entryName).getName();
							String target = HdfsUtil.path(targetPath, cleandEntryName);

							log.info("parseAndUpdateInputParams: HDFS: targetPath: "+targetPath);
							log.info("parseAndUpdateInputParams: HDFS: cleandEntryName: "+cleandEntryName);
							log.info("parseAndUpdateInputParams: HDFS: target: "+target);

							HdfsUtil.put(tmpFile, target);

							if (inputParam.isFolder()) {
								// folder
								props.put(fieldName, HdfsUtil.makeAbsolute(HdfsUtil.path(hdfsWorkspace, fieldName)));
							} else {
								// file
								props.put(fieldName, HdfsUtil.makeAbsolute(HdfsUtil.path(hdfsWorkspace, fieldName, cleandEntryName)));
							}

						} else { // local
							// copy to workspace in temp directory
							String targetPath = FileUtil.path(localWorkspace, "input", fieldName);
							FileUtil.createDirectory(targetPath);
							String cleandEntryName = new File(entryName).getName();
							String target = FileUtil.path(targetPath, cleandEntryName);

							log.info("parseAndUpdateInputParams: local: targetPath: "+targetPath);
							log.info("parseAndUpdateInputParams: local: cleandEntryName: "+cleandEntryName);
							log.info("parseAndUpdateInputParams: local: target: "+target);
							
							FileUtil.copy(tmpFile, target);

							if (inputParam.isFolder()) {
								// folder
								if (inputParam.getPattern() != null && !inputParam.getPattern().isEmpty()) {
									props.put(fieldName, new File(targetPath).getAbsolutePath());
								} else {
									props.put(fieldName, new File(targetPath).getAbsolutePath());
								}
							} else {
								// file
								props.put(fieldName, new File(target).getAbsolutePath());
							}

						}

						// deletes temporary file
						FileUtil.deleteFile(tmpFile);

					} catch (FileUploadIOException e) {
						file.delete();
						throw e;
					} catch (Exception e) {
						file.delete();
						return null;

					}

				} else { // entryName == null

					String key = StringEscapeUtils.escapeHtml(item.getFieldName());
					if (key.startsWith("input-")) {
						key = key.replace("input-", "");
					}

					WdlParameterInput input = getInputParamByName(app, key);

					if (!key.equals(PARAM_JOB_NAME) && input == null && !key.equals("total_chunks") && !key.equals("cur_chunk") && !key.equals("jobid")) {
					    log.info("parseAndUpdateInputParams: entryName=null: key='"+key+"' not found");
					    throw new FileUploadException("entryName==null, parameter '" + key + "' not found.");
					}

					String value = StringEscapeUtils.escapeHtml(Streams.asString(item.openStream()));
					log.info("parseAndUpdateInputParams: entryName=null: key="+key+", value="+value);

					/* add chunk information, jobid */
					if (key.equals("total_chunks") || key.equals("cur_chunk") || key.equals("jobid")){
					    params.put(key,value);
					}

					if (input != null && input.isFileOrFolder() && ImporterFactory.needsImport(value)) {
						throw new FileUploadException("Parameter '" + input.getId() + "': URL-based uploads are no longer supported. Please use direct file uploads instead.");
					}

					if (!props.containsKey(key)) {
						// don't override uploaded files
						props.put(key, value);
					}
				}
			}
		} catch (FileUploadIOException e) {
			throw e;
		} catch (FileUploadException e) {
			throw e;
		} catch (Exception e) {
			return null;

		}

		for (WdlParameterInput input : app.getWorkflow().getInputs()) {
			if (!params.containsKey(input.getId())) {
				if (props.containsKey(input.getId())) {
					if (input.isFolder() && input.getPattern() != null && !input.getPattern().isEmpty()) {
						String pattern = props.get(input.getId() + "-pattern");
						String value = props.get(input.getId());
						if (!value.endsWith("/")) {
							value = value + "/";
						}
						params.put(input.getId(), value + pattern);
					} else {

						if (input.getTypeAsEnum() == WdlParameterInputType.CHECKBOX) {
							params.put(input.getId(), input.getValues().get("true"));
						} else {
							params.put(input.getId(), props.get(input.getId()));
						}
					}
				} else {
					// ignore invisible input parameters
					if (input.getTypeAsEnum() == WdlParameterInputType.CHECKBOX && input.isVisible()) {
						params.put(input.getId(), input.getValues().get("false"));
					}
				}
			}
		}

		params.put(PARAM_JOB_NAME, props.get(PARAM_JOB_NAME));
		
		for (String key: params.keySet()){
		    log.info("parseAndUpdateInputParams: output params: key="+key+", value="+params.get(key));
		}

		return params;
	}

    private Map<String, String> parseAndUpdateInputParams_chunks(Representation entity, WdlApp app, String hdfsWorkspace, String localWorkspace) throws FileUploadIOException, FileUploadException {
	Map<String, String> props = new HashMap<String, String>();
	Map<String, String> props_tmp_hdfs_files = new HashMap<String, String>();
	Map<String, String> props_tmp_local_files = new HashMap<String, String>();
	List<String> props_tmp_hdfs_folders = new ArrayList<String>();
	List<String> props_tmp_local_folders = new ArrayList<String>();
	Map<String, String> params = new HashMap<String, String>();
	Map<String, String> hdfs_files=new HashMap<String, String>();
	Map<String, String> local_files=new HashMap<String, String>();

	FileItemIterator iterator = null;
	try {
	    iterator = parseRequest(entity);
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
		
	try {
	    while (iterator.hasNext()) {
		FileItemStream item = iterator.next();
		String entryName = StringEscapeUtils.escapeHtml(item.getName());

		if (entryName != null) {  // file parameter
		    log.info("parseAndUpdateInputParams: entryName: "+entryName);
		    File file = null;

		    try {
			String tmpFile = getSettings().getTempFilename(entryName);
			log.info("parseAndUpdateInputParams: tmpFile: "+tmpFile);
			file = new File(tmpFile);

			// read file from stream
			FileUtils.copyInputStreamToFile(item.openStream(), file);

			// remove upload identification!
			String fieldName = item.getFieldName().replace("-upload", "").replace("input-", ""); // e.g., "files"
			log.info("parseAndUpdateInputParams: fieldName: "+fieldName);
			
			WdlParameterInput inputParam = getInputParamByName(app,fieldName);
			if (inputParam == null) {
			    throw new Exception("after calling getInputParamByName, parameter '" + fieldName + "' not found.");
			}

			if (inputParam.isHdfs()) {
			    String targetPath = HdfsUtil.path(hdfsWorkspace,fieldName);
			    String cleandEntryName = new File(entryName).getName();
			    String target = HdfsUtil.path(targetPath,cleandEntryName);

			    log.info("parseAndUpdateInputParams: HDFS: targetPath: "+targetPath);
			    log.info("parseAndUpdateInputParams: HDFS: cleandEntryName: "+cleandEntryName);
			    log.info("parseAndUpdateInputParams: HDFS: target: "+target);

			    // copy to target dir
			    HdfsUtil.put(tmpFile, target); // modified
			    hdfs_files.put(target,HdfsUtil.path(fieldName,cleandEntryName)); // only the part that needs to be appended to workspace

			    if (inputParam.isFolder()) { // modified
				// folder
				props.put(fieldName, HdfsUtil.makeAbsolute(HdfsUtil.path(hdfsWorkspace, fieldName)));
				props_tmp_hdfs_folders.add(fieldName);
			    } else {
				// file
				props.put(fieldName, HdfsUtil.makeAbsolute(HdfsUtil.path(hdfsWorkspace, fieldName, cleandEntryName)));
				props_tmp_hdfs_files.put(fieldName,cleandEntryName);
			    }

			} else { // local
			    // copy to workspace
			    String targetPath = FileUtil.path(localWorkspace, "input", fieldName);
			    FileUtil.createDirectory(targetPath);
			    String cleandEntryName = new File(entryName).getName();
			    String target = FileUtil.path(targetPath, cleandEntryName);

			    log.info("parseAndUpdateInputParams: local: targetPath: "+targetPath);
			    log.info("parseAndUpdateInputParams: local: cleandEntryName: "+cleandEntryName);
			    log.info("parseAndUpdateInputParams: local: target: "+target);
							
			    FileUtil.copy(tmpFile, target); // modified
			    local_files.put(target,FileUtil.path("input",fieldName,cleandEntryName));

			    if (inputParam.isFolder()) { // modified
				// folder
				props.put(fieldName, new File(targetPath).getAbsolutePath());
				props_tmp_local_folders.add(fieldName);
			    } else {
				// file
				props.put(fieldName, new File(target).getAbsolutePath());
				props_tmp_local_files.put(fieldName,cleandEntryName);
			    }
			}

			// delete temporary file
			FileUtil.deleteFile(tmpFile);

		    } catch (FileUploadIOException e) {
			file.delete();
			throw e;
		    } catch (Exception e) {
			file.delete();
			return null;
		    }
		} else { // entryName == null
		    String key = StringEscapeUtils.escapeHtml(item.getFieldName());
		    if (key.startsWith("input-")) {
			key = key.replace("input-", "");
		    }

		    WdlParameterInput input = getInputParamByName(app,key);
		    // deal with extra parameters that are not part of WDL specs
		    if (!key.equals(PARAM_JOB_NAME) && input == null && !key.equals("total_chunks") && !key.equals("cur_chunk") && !key.equals("jobid") && !key.equals("lws") && !key.equals("hws")) {
			log.info("parseAndUpdateInputParams: entryName=null: key='"+key+"' not found");
			throw new FileUploadException("entryName==null, parameter '" + key + "' not found.");
		    }

		    String value = StringEscapeUtils.escapeHtml(Streams.asString(item.openStream()));
		    log.info("parseAndUpdateInputParams: entryName=null: key="+key+", value="+value);

		    /* add chunk information, jobid */
		    if (key.equals("total_chunks") || key.equals("cur_chunk") || key.equals("jobid") || key.equals("lws") || key.equals("hws")){
			params.put(key,value);
		    }

		    if (input != null && input.isFileOrFolder() && ImporterFactory.needsImport(value)) {
			throw new FileUploadException("Parameter '" + input.getId() + "': URL-based uploads are no longer supported. Please use direct file uploads instead.");
		    }

		    // don't override uploaded files
		    if (!props.containsKey(key)) {
			props.put(key, value);
		    }
		}
	    }
	} catch (FileUploadIOException e) {
	    throw e;
	} catch (FileUploadException e) {
	    throw e;
	} catch (Exception e) {
	    return null;
	}

	// ---------------

	// if jobid is not in input, add jobid --> "NA" mapping
	if (!params.containsKey("jobid")){
	    params.put("jobid","NA");
	}
	if (!params.containsKey("lws")){
	    params.put("lws","NA");
	}
	if (!params.containsKey("hws")){
	    params.put("hws","NA");
	}
	if (!params.containsKey("cur_chunk")){
	    params.put("cur_chunk","1");
	}
	if (!params.containsKey("total_chunks")){
	    params.put("total_chunks","1");
	}
	
	String jobid=params.get("jobid");
	if (!jobid.equals("NA")){
	    log.info("got jobid="+jobid+", need to copy files to new workspace");
	    // HDFS workspace
	    String newHdfsWorkspace=null;
	    try {
		newHdfsWorkspace=HdfsUtil.path(getSettings().getHdfsWorkspace(),jobid);
		log.info("new HDFS Workspace: "+newHdfsWorkspace);
		// copy files to the new workspace
		for (Map.Entry<String, String> entry: hdfs_files.entrySet()){
		    String oldfile=entry.getKey();
		    String newfile=HdfsUtil.path(newHdfsWorkspace,entry.getValue());
		    log.info("HDFS: copying "+oldfile+" to "+newfile);
		    HdfsUtil.put(oldfile,newfile);
		}
		// delete old workspace
		log.info("deleting old HDFS workspace: "+hdfsWorkspace);
		HdfsUtil.delete(hdfsWorkspace); // ?
		// update props
		for (String entry: props_tmp_hdfs_folders){
		    props.put(entry,HdfsUtil.makeAbsolute(HdfsUtil.path(newHdfsWorkspace,entry)));
		}
		for (Map.Entry<String, String> entry: props_tmp_hdfs_files.entrySet()){
		    props.put(entry.getKey(),HdfsUtil.makeAbsolute(HdfsUtil.path(newHdfsWorkspace,entry.getKey(),entry.getValue())));
		}		
	    } catch (NoClassDefFoundError e) {
		log.warn("Hadoop not found in classpath. Ignore HDFS Workspace.");
	    }
	    // local workspace
	    String newLocalWorkspace=FileUtil.path(getSettings().getLocalWorkspace(),jobid);
	    FileUtil.createDirectory(newLocalWorkspace);
	    log.info("new local Workspace: "+newLocalWorkspace);
	    for (Map.Entry<String, String> entry: local_files.entrySet()){
		String oldfile=entry.getKey();
		String newfile=FileUtil.path(newLocalWorkspace,entry.getValue());
		log.info("local: copying "+oldfile+" to "+newfile);
		FileUtil.copy(oldfile,newfile);
	    }
	    // delete old workspace
	    log.info("deleting old local workspace: "+localWorkspace);
	    FileUtil.deleteDirectory(localWorkspace);
	    // update props
	    for (String entry: props_tmp_local_folders){
		props.put(entry,new File(FileUtil.path(newLocalWorkspace,"input",entry)).getAbsolutePath());
	    }
	    for (Map.Entry<String, String> entry: props_tmp_local_files.entrySet()){
		props.put(entry.getKey(),new File(FileUtil.path(FileUtil.path(newLocalWorkspace,"input",entry.getKey()),entry.getValue())).getAbsolutePath());
	    }
	} // jobid != NA
	
	// ---------------
	
	for (WdlParameterInput input : app.getWorkflow().getInputs()) {
	    if (!params.containsKey(input.getId())) {
		if (props.containsKey(input.getId())) {
		    if (input.isFolder() && input.getPattern() != null && !input.getPattern().isEmpty()) {
			String pattern = props.get(input.getId() + "-pattern");
			String value = props.get(input.getId());
			if (!value.endsWith("/")) {
			    value = value + "/";
			}
			params.put(input.getId(), value + pattern);
		    } else {
			if (input.getTypeAsEnum() == WdlParameterInputType.CHECKBOX) {
			    params.put(input.getId(), input.getValues().get("true"));
			} else {
			    params.put(input.getId(), props.get(input.getId()));
			}
		    }
		} else {
		    // ignore invisible input parameters
		    if (input.getTypeAsEnum() == WdlParameterInputType.CHECKBOX && input.isVisible()) {
			params.put(input.getId(), input.getValues().get("false"));
		    }
		}
	    }
	}

	params.put(PARAM_JOB_NAME, props.get(PARAM_JOB_NAME));
		
	for (String key: params.keySet()){
	    log.info("parseAndUpdateInputParams: output params: key="+key+", value="+params.get(key));
	}

	return params;
    }
    
	private FileItemIterator parseRequest(Representation entity) throws FileUploadException, IOException {

		// 1/ Create a factory for disk-based file items
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(1000240);

		// 2/ Create a new file upload handler based on the Restlet
		// FileUpload extension that will parse Restlet requests and
		// generates FileItems.
		RestletFileUpload upload = new RestletFileUpload(factory);
		Settings settings = getSettings();
		upload.setFileSizeMax(settings.getUploadLimit());
		return upload.getItemIterator(entity);

	}

	private WdlParameterInput getInputParamByName(WdlApp app, String name) {

		for (WdlParameterInput input : app.getWorkflow().getInputs()) {
			if (input.getId().equals(name)) {
				return input;
			}
		}
		return null;
	}

}
