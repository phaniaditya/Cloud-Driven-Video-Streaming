package com.project.server;

/*VideoStream
  Team 16*/

import java.io.*;

public class VideoStream {

  FileInputStream fileinputstream; 
  int framenumb; 

  
  //constructor
 
  public VideoStream(String filename) throws Exception{

   
    fileinputstream = new FileInputStream(filename);
    framenumb = 0;
  }

  //The getnextframe returns the size of the frame and the next frame as an array of byte
  
  public int etnextframe(byte[] frame) throws Exception
{
 int ret = fileinputstream.read(frame);
if( ret != -1){

return ret;
}
else
{
return 0;
}
}
  public int getnextframe(byte[] frame) throws Exception
  {
    int length = 0;
    String length_string;
    byte[] frame_length = new byte[5];

    //The current frame length is read
    fileinputstream.read(frame_length,0,5);
    int i=0;
    for(i=0;i<5;i++)
    {
    	System.out.println(frame_length[i]);
    }
	
    //transformation of frame_length to integer is done
   
    length_string =new String(frame_length);
    length = Integer.parseInt(length_string);
    System.out.println("......."+length);

	
    return(fileinputstream.read(frame,0,length));
  }
}
