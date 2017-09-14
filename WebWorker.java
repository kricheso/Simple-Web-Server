/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/



// Imports
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;


//Start Class
public class WebWorker implements Runnable {

    //Global vars
    private Socket socket;
    String format;
    private String myPath = " ";

    //Constructor
    public WebWorker(Socket s) {
       socket = s;
    }

    // ==================================================================================
    /* Worker thread starting point. Each worker handles just one HTTP
     request and then returns, which destroys the thread. This method
     assumes that whoever created the worker created it with a valid
     open socket object. */
    
    
    public void run(){
        
        System.err.println("Handling connection...");

        try {
            InputStream  is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            String path = readHTTPRequest(is);
            
            //Path includes addition /, remove this dash
            path = path.substring(1);

//            writeHTTPHeader(os,"text/html",path);
//            writeContent(os,path);
//            os.flush();
//            socket.close();
            
            
            //We gotta format the proper picture
            //Lets use if else statements!
            if(path.endsWith(".jpg"))
                
                format = "image/jpg";
            
            else if(path.endsWith(".jpeg"))
                
                format = "image/jpeg";
            
            else if (path.endsWith(".gif"))
                
                format = "image/gif";
            
            else if(path.endsWith(".png"))
                
                format = "image/png";
            
            else if(path.endsWith(".ico"))
                
                format = "image/x-icon";
            
            else
                
                format = "text/html";
            
            //We now have the right value in format
            writeHTTPHeader(os,format, path);
            writeContent(os, path);
            os.flush();
            socket.close();
        }
        catch (Exception e) {
            System.err.println("Output error: "+e);
        }
        System.err.println("Done handling connection.");
        return;
        
    }
    
    
    // ==================================================================================
    /* Read the HTTP request header. */
    
    
    private String readHTTPRequest(InputStream is) {

        String line;
        String GetLine = "";

        
        BufferedReader r = new BufferedReader(new InputStreamReader(is));

        while (true) {

            try {
                while (!r.ready()) Thread.sleep(1);
                line = r.readLine();
                
                //Print every line to the terminal
                System.err.println("Request line: ("+line+")");

                //Check if the line is "GET"
                if(line.length() > 0){
                    GetLine = line.substring(0,3);
                }
                
                /* Try to find the location and store it Since we
                 know that the path is printed after the word GET, we
                 can crop that with substring and get the path! */
                if(GetLine.equals("GET")){
                    myPath = line.substring(4);
                    myPath = myPath.substring(0, myPath.indexOf(" "));
                    System.err.println("Requested pwd is: " + myPath);
                }

                //if done reading the lines, stop.
                if (line.length()==0) break;
                
            } //end try
            
            catch (Exception e) {
                System.err.println("Request error: " + e);
                break;
            }
            
        } //end while
        
        return myPath;
    }
    
    
    // ==================================================================================
    /* Write the HTTP header lines to the client network connection.
     @param os is the OutputStream object to write to 
     @param contentType is the string MIME content type (e.g. "text/html") */
    
    
    private void writeHTTPHeader(OutputStream os, String contentType, String path) throws Exception
    {
    
        
        Date d = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        //Checking if file exists write request as okay
        File f = new File(path);
        
        if(f.exists() && !f.isDirectory()){
            os.write("HTTP/1.1 200 OK\n".getBytes());
        }
        else {
            os.write("HTTP/1.1 404 not found\n".getBytes());
        }
        
        os.write("Date: ".getBytes());
        os.write((df.format(d)).getBytes());
        os.write("\n".getBytes());
        os.write("Server: Kousei's very own server\n".getBytes());
        os.write("Connection: close\n".getBytes());
        os.write("Content-Type: ".getBytes());
        os.write(contentType.getBytes());
        os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
        
        
        
        return;
    }
    
    
    // ==================================================================================

    /**
    * Write the data content to the client network connection. This MUST
    * be done after the HTTP header has been written out.
    * @param os is the OutputStream object to write to
    **/
    private void writeContent(OutputStream os, String path) throws Exception
    {
        
        File noice = new File(path);
        
        if(noice.exists() && !noice.isDirectory()){
            FileInputStream myStream = new FileInputStream(path);
            BufferedReader r = new BufferedReader(new InputStreamReader(myStream));
            
            
            if(format.startsWith("image/"))
                
                Files.copy(noice.toPath(), os);
            
            
            
            else{
                
                String myFile;

                //Reading file
                while ((myFile = r.readLine()) != null){
                    
                    //Check for <cs371date> tag and replace
                    if(myFile.toLowerCase().contains(("<cs371date>").toLowerCase()) == true){
                        
                        Date myDate = new Date();
                        String s = myFile.replaceAll("<cs371date>", myDate.toString());
                        
                        os.write(s.getBytes());
                        os.write("<r>".getBytes());
                    }
                    
                    
                    //Check for <cs371server> tag & replace
                    if(myFile.toLowerCase().contains(("<cs371server>").toLowerCase()) == true){
                        
                        String serverID = "Kousei Richeson's Server";
                        String s = myFile.replaceAll("<cs371server>", serverID);
                        os.write(s.getBytes());
                        os.write("<r>".getBytes());
                    }
                    
                    
                    os.write(myFile.getBytes());
                }
                
                r.close();
            }
        }
            
        
        
        else{
            os.write("<h3>Error: 404 not Found</h3>".getBytes());
        }
        
        
        
        
        
        
    //   os.write("<html><head></head><body>\n".getBytes());
    //   os.write("<h3>My web server works!</h3>\n".getBytes());
    //   os.write("</body></html>\n".getBytes());
    }

} // end class
