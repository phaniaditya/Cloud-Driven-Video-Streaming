package com.project.server;

/* ------------------
   Server program
   Team 16
   ---------------------- */


import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Server extends JFrame implements ActionListener,Runnable{

 private static final long serialVersionUID = 1L;
//RTP variables
  
   DatagramSocket RTPsocket; // setting the socket to send and receive UDP packets
  DatagramPacket send_datapacket; //UDP packet

   InetAddress client_ipaddr; // Assigning Client IP address
   int RTPdestinport = 0; //destination port for RTP packets which is given by the RTSP Client is initialized to 0

  //GUI
  JLabel label;

  //video variables
 
  int image_number = 0; 
   VideoStream vid; 
   int MJPEG_TYPE = 26; 
   int FRAMEPERIOD = 100; 
   int vid_LENGTH = 500; 

   Timer timer; //setting timer 
  byte[] buffer; //buffer

  //RTSP variables and states
  
  final  int INIT = 0;
  final  int READY = 1;
  final  int PLAYING = 2;

  //rtsp message types
  final  int SETUP = 3;
  final  int PLAY = 4;
  final  int PAUSE = 5;
  final  int TEARDOWN = 6;

   int state; 
   Socket RTSPsocket; //socket creation
  //input and output stream filters are set
   BufferedReader RTSPBufferedReader;
   BufferedWriter RTSPBufferedWriter;
   String vidFileName; 
   int RTSPID = 123456; 
   int RTSP_sequence_num = 0; 
  int RTSPport = 20001;
  InetAddress ServerIPAddr;
  final  String CRLF = "\r\n";
  ServerSocket listenSocket;

  
  public Server(){

    
    super("Server");

    //Timer
    timer = new Timer(FRAMEPERIOD, this);
    timer.setInitialDelay(0);
    timer.setCoalesce(true);

    //memory is allocated for the sending buffer
    buffer = new byte[15000]; 

    //Handler to close the main window
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	//stop the timer 
	timer.stop();
	try {
		System.out.println("Closing Socket associated to Port "+listenSocket.getLocalPort());
		listenSocket.close();
	} catch (IOException e1) {
		e1.printStackTrace();
	}
	
      }});

    //GUI
    label = new JLabel("Send frame #        ", JLabel.CENTER);
    getContentPane().add(label, BorderLayout.CENTER);
  }
          
  public int getRTSPport() {
	return RTSPport;
}

public void setRTSPport(int rTSPport) {
	RTSPport = rTSPport;
}

void doWork(Socket s) throws Exception{
	 client_ipaddr = RTSPsocket.getInetAddress();
	
    
    state = INIT;

    //Set input and output stream filters
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()) );
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()) );

    int request_type;
    boolean done = false;
    while(!done)
      {
    	
	request_type = parse_RTSP_request(); 

	if (request_type == SETUP)
	  {
		 
	    done = true;

	    //update RTSP state
	    state = READY;
	    
	    System.out.println("New RTSP state: REDY");
   
	    //Send response
	    send_RTSP_response();
   
	    //The video Stream object:
	    VideoStream vs=new VideoStream(vidFileName);
	    vid = vs;
	    

	    //RTP socket
	    RTPsocket = new DatagramSocket();
	    RTPsocket.setReuseAddress(true);
	  }
      }

     //loop to handle RTSP requests
    while(true)
      {
	
	request_type = parse_RTSP_request(); 
	    
	if ((request_type == PLAY) && (state == READY))
	  {
	    //send back response
	    send_RTSP_response();
	    //start timer
	    try{
	    timer.start();
	    //update state
	    state = PLAYING;
	    }catch(Exception e){e.printStackTrace();}
	    System.out.println("New RTSP state: PLAYING");
	  }
	else if ((request_type == PAUSE) && (state == PLAYING))
	  {
	    //send back response
	    send_RTSP_response();

	    //stop timer
	    timer.stop();

	    //update state
	    state = READY;
	    System.out.println("New RTSP state: READY");
	  }
	else if (request_type == TEARDOWN)
	  {
	    //send back response
	    send_RTSP_response();

	    //stop timer
	    timer.stop();
	    
	    try {
			RTSPsocket.close();
		} catch (IOException e) 
{
			e.printStackTrace();
		}
	    RTPsocket.close();
	    done = true;
	   
	  }
      }
}


  //------------------------
  //Handler for timer
  //------------------------
  public void actionPerformed(ActionEvent e) {
    
    if (image_number < vid_LENGTH)
      {
	
	image_number++;
       
	try {
	  
	  int image_length = vid.getnextframe(buffer);
System.out.println(image_length);
	  
	  RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, image_number, image_number*FRAMEPERIOD, buffer, image_length);
	  
	  
	  int packet_length = rtp_packet.getlength();

	  
	  byte[] packet_bits = new byte[packet_length];
	  rtp_packet.getpacket(packet_bits);

	  
	  send_datapacket = new DatagramPacket(packet_bits, packet_length, client_ipaddr, RTPdestinport);
	  RTPsocket.send(send_datapacket);

	  
	  rtp_packet.printheader();

	  //updating GUI
	  label.setText("Send frame #" + image_number);
	}
	catch(Exception ex)
	  {
	    ex.printStackTrace();
	    System.exit(0);
	  }
}
    else
      {
	//stop the timer
	timer.stop();
      }
  }

  
  //Parse RTSP Request

  private  int parse_RTSP_request()
  {
    int request_type = -1;
    try{
      
      String RequestLine = RTSPBufferedReader.readLine();
      
      System.out.println(RequestLine);

      StringTokenizer tokens = new StringTokenizer(RequestLine);
      String request_type_string = tokens.nextToken();

      
      if ((new String(request_type_string)).compareTo("SETUP") == 0)
	request_type = SETUP;
      else if ((new String(request_type_string)).compareTo("PLAY") == 0)
	request_type = PLAY;
      else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
	request_type = PAUSE;
      else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
	request_type = TEARDOWN;

      if (request_type == SETUP)
	{
	  //The vidFileName from RequestLine is requested
	  vidFileName = tokens.nextToken();
	}

      
      String SeqNumLine = RTSPBufferedReader.readLine();
      System.out.println(SeqNumLine);
      tokens = new StringTokenizer(SeqNumLine);
      tokens.nextToken();
      RTSP_sequence_num = Integer.parseInt(tokens.nextToken());
	
      
      String LastLine = RTSPBufferedReader.readLine();
      System.out.println(LastLine);

      if (request_type == SETUP)
	{
	  
	  tokens = new StringTokenizer(LastLine);
	  for (int i=0; i<3; i++)
	    tokens.nextToken(); 
	  RTPdestinport = Integer.parseInt(tokens.nextToken());
	}
      
    }
    catch(Exception ex)
      {
	System.out.println("Exception caught2: "+ex);
	request_type = TEARDOWN;
	
      }
    return(request_type);
  }

  
  //Send RTSP Response
 
  private  void send_RTSP_response()
  {
    try{
      RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
      RTSPBufferedWriter.write("CSeq: "+RTSP_sequence_num+CRLF);
      RTSPBufferedWriter.write("Session: "+RTSPID+CRLF);
      RTSPBufferedWriter.flush();
      
    }
    catch(Exception ex)
      {
    	System.out.println("Exception caught3: "+ex);
	
      }
  }

public InetAddress getServerIPAddr() {
	return ServerIPAddr;
}

public void setServerIPAddr(InetAddress serverIPAddr) {
	ServerIPAddr = serverIPAddr;
}

@Override
	public void run() {
		
		try {
			listenSocket = new ServerSocket(RTSPport, 5, ServerIPAddr);
			listenSocket.setReuseAddress(true);
			System.out.println("Server is waiting on the Port "+RTSPport+" for client");
			RTSPsocket = listenSocket.accept();
			doWork(RTSPsocket);
			
		} catch (SocketException e) {
			System.out.println("Socket closed");
		} catch (IOException e) {
			e.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}

}
}
