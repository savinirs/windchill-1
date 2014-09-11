import java.io.*;
import java.net.*;
/**
 * 
 */

/**
 * @author dxu
 *
 */
public class DownGrok {

	private static final String hostXref="http://ah-grok.ptcnet.ptc.com/xref/";
	private static final String hostRaw ="http://ah-grok.ptcnet.ptc.com/raw/";
	private static final String hrefStart = "a href=\"";
	private static final String hrefFileEnd = "\" class=\"p\"";
	private static final String hrefFolderEnd = "\" class=\"r\"";
	
	private static String thisURL = "";
	
	private static String module = "";
	private static String folderName = "src";
	
	/**
	 * @param args
	 * Sample: http://ah-grok.ptcnet.ptc.com/xref/x-24-M022/Windchill/ src
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello World!");
		
		try{
			parserCommandLine(args);
			
			if(!validateURL(thisURL)){
				System.out.println("Command List error");
				return ;
			}
			
			File directory = new File(".");
			String currentFullPath = directory.getAbsolutePath();
			System.out.println("Current Path is: "+currentFullPath);
			
			if(exportOneModule(thisURL,currentFullPath,module,folderName)){
				System.out.println("Successfully Download everything!");
			}else{
				System.out.println("There is something wrong happened!");
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * this function will check the content of current module and
	 * will continue to search sub-module and download all files within this module.
	 * @param fullModulePath
	 * @param localFullPath
	 * @param localSubFolder
	 * @return
	 */
	private static boolean exportOneModule(String fullModulePath,String localFullPath,String currentModule, String localSubFolder){
		
		String nextModule;
		
		if(("").equals(localSubFolder)){
			return false;
		}else{
			if(!confirmOrCreateLocalFolder(localFullPath,localSubFolder)){
				return false;
			}
		}
		localFullPath = localFullPath+"/"+localSubFolder;
		
		BufferedInputStream in=null;
		StringBuffer sBuffer = new StringBuffer();
		
		try{

			URL url = new URL(fullModulePath);
			URLConnection conn = url.openConnection();
			
			in = new BufferedInputStream(conn.getInputStream());
			System.out.print("Start reading ...");
			int byteRead;
			byte[] contents = new byte[2048];
			String tempContents;
			
			while ((byteRead = in.read(contents)) !=-1 ){
				tempContents=new String(contents,0,byteRead);
				sBuffer.append(tempContents);
				System.out.print("...");
			}
			System.out.println("Done!");
			in.close();
			
			int startPosition=-1;
			int endPosition=-1;
			
			startPosition = sBuffer.indexOf("<table");
			endPosition = sBuffer.indexOf("</table>");
			String tableContent;
			
			if(startPosition != -1 && endPosition != -1){
				tableContent = sBuffer.substring(startPosition, endPosition);
			}else{
				System.out.println("Can't find flag in content!");
				return false;
			}
			
			//deal with files.
			endPosition = tableContent.indexOf(hrefFileEnd);
			
			while(endPosition !=-1){
				startPosition = tableContent.lastIndexOf(hrefStart,endPosition-1);
				
				if(startPosition != -1 && endPosition-startPosition -hrefStart.length() > 0 ){
					String moduleOrFile=tableContent.substring(startPosition+hrefStart.length(),endPosition);
					System.out.print("Start with File Name is: "+moduleOrFile+"...");
					
					if(moduleOrFile.lastIndexOf("/")==moduleOrFile.length()-1){
						//this is a module
						nextModule = moduleOrFile.substring(0,moduleOrFile.length()-1);
						if(!exportOneModule(fullModulePath+"/"+moduleOrFile,localFullPath,nextModule,nextModule)){
							System.out.println("Failed in module "+ moduleOrFile);
							return false;
						}
					}else{
						if(moduleOrFile.indexOf(".")>0){
							if(!exportOneFile(fullModulePath,moduleOrFile,localFullPath)){
								System.out.println("Failed in exporting file:"+moduleOrFile);
								return false;
							}
						}
					}
				}
				endPosition = tableContent.indexOf(hrefFileEnd,endPosition+hrefFileEnd.length());
				
			}
			
			
			
			
			//deal with new module
			
			endPosition = tableContent.indexOf(hrefFolderEnd);
			
			while(endPosition !=-1){
				startPosition = tableContent.lastIndexOf(hrefStart,endPosition-1);
				//System.out.println("Start Position is:"+startPosition+"\tand end position is:"+endPosition);
				
				if(startPosition != -1 && endPosition-startPosition -hrefStart.length() > 0 ){
					String moduleOrFile=tableContent.substring(startPosition+hrefStart.length(),endPosition);
					System.out.println("Start with Module is: "+moduleOrFile);
					
					if(moduleOrFile.lastIndexOf("/")==moduleOrFile.length()-1){
						//this is a module
						nextModule = moduleOrFile.substring(0,moduleOrFile.length()-1);
						if(!exportOneModule(fullModulePath+"/"+moduleOrFile,localFullPath,nextModule,nextModule)){
							System.out.println("Failed in module "+ moduleOrFile);
							return false;
						}
					}else{
						if(moduleOrFile.indexOf(".")>0){
							if(!exportOneFile(fullModulePath,moduleOrFile,localFullPath)){
								System.out.println("Failed in exporting file:"+moduleOrFile);
								return false;
							}
						}
					}
				}
				endPosition = tableContent.indexOf(hrefFolderEnd,endPosition+hrefFolderEnd.length());
				
			}
			
			
			return true;
		}catch(Exception e){
			e.printStackTrace();
			try{
				if(in != null) in.close();
				
			}catch(IOException ioe){
				ioe.printStackTrace();
				return false;
			}
		}
		try{
			if(in != null) in.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * export single file in local path
	 * @param fullFilePath
	 * @param fileName
	 * @param localFullPath
	 * @return
	 */
	
	private static boolean exportOneFile(String fullFilePath, String fileName, String localFullPath){
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		try{
			fullFilePath = fullFilePath.replace(hostXref,hostRaw)+"/"+fileName;
			
			URL url = new URL(fullFilePath);
			URLConnection conn = url.openConnection();
					
			in = new BufferedInputStream(conn.getInputStream());
			out = new BufferedOutputStream(new FileOutputStream(localFullPath+"/"+fileName));
			int byteRead;
			byte[] buff = new byte[2*2048];
			
			while((byteRead=in.read(buff))>0){
				System.out.print("...");
				out.write(buff,0,byteRead);
			}
			
			in.close();
			out.close();
			System.out.println("Done!");
			
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				if(in!=null)
					in.close();
				if(out!=null)
					out.close();
				
			}catch(Exception finalE){
				finalE.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * this function will check current folder exist or not, otherwise, it will create it.
	 * @param localSubFolder
	 * @param localFullPath
	 * @return
	 */
	private static boolean confirmOrCreateLocalFolder(String localFullPath, String localSubFolder){
		File theFile = new File(localFullPath + "/" + localSubFolder+"/");
		System.out.print("\n\tTo create folder:"+localSubFolder);
		
		//theFile.mkdirs();
		//following code is not a good practice but it will only be used here temporarly.
		
		if(theFile.mkdirs()){
			System.out.println("\t...\tSuccessfully created the folder!");
			return true;
		}else{
			System.out.println("Something is wrong since there is a file with same name");
			return false;
		}
	
	}
	
	/**
	 * @param args
	 * Parser command line input
	 * First parameter must be the root URL of a module
	 * Second parameter can be a user name
	 * Third parameter can be a password
	 */
	private static void parserCommandLine(String[] args){
		try{
			if(args.length>0){
				thisURL = args[0];
			}
			if(args.length>1){
				folderName=args[1];
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @author dxu
	 * @param url
	 * to valid the url is a qualified url or not.
	 * 1) start from xref host; 2) end with a model
	 * @return true -> success; false - failed.
	 *
	 */
	private static boolean validateURL(String url){
		int position = url.indexOf(hostXref);
		
		if(position != 0){ //should start with host Xref
			System.out.println("URL must start with "+hostXref);
			return false;
		}
		
		String currentModule="";
		
		if(url.lastIndexOf("/")==url.length()-1){
			currentModule = url.substring(url.lastIndexOf("/",url.length()-2)+1,url.length()-1);
			//System.out.println("length="+url.length()+"\tcurrentModule("+currentModule+")");
		}else{
			currentModule = url.substring(url.lastIndexOf("/")+1);
			//System.out.println("another length="+url.length()+"\tcurrentModule("+currentModule+")");
		}

		if(currentModule == null || ("").equals(currentModule)){
			System.out.println("You should not start from a root!");
			return false;
		}
		
		if(currentModule.indexOf(".") != -1){
			System.out.println("We don't support a dirrect download of single file! -->" + currentModule);
			return false;
		}
		module = currentModule;
		
		return true;
		
	}
	
}
