package com.project.client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client{

  //GUI set up
  JFrame f = new JFrame("Client");
  JButton setupButton = new JButton("Setup");
  JButton playButton = new JButton("Play");
  JButton pauseButton = new JButton("Pause");
  JButton tearButton = new JButton("Stop");
  JPanel mainPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JLabel iconLabel = new JLabel();
  ImageIcon icon;


  //RTP variables,client set up
  DatagramPacket recv_dp;
  DatagramSocket RTPsocket;
  int RTP_RECV_PORT = 25000;
  Timer timer;
  byte[] buffer;

  //RTSP set up variables
  final static int INIT = 0;
  final static int READY = 1;
  final static int PLAYING = 2;
  static int state;
  Socket RTSP_socket;
  //input and output buffer
  static BufferedReader RTSPBufferedReader;
  static BufferedWriter RTSPBufferedWriter;
  static String video_filename;
  int RTSP_sequence_num = 0;
  int RTSP_id = 0; //Session ID
  static int RTSP_server_port = 20001;
  final static String CRLF = "\r\n"; //

  //Video constants:
  //------------------
  static int MJPEGTYPE = 26; //RTP payload type for MJPEG video

  //--------------------------
  //Constructor
  //--------------------------
  public Client() {

	  	//build GUI
	  	//--------------------------

	  	//Frame
	  	f.addWindowListener(new WindowAdapter() {
	  		public void windowClosing(WindowEvent e) {
	  				System.exit(0);
	  				}
	  	});

	  	//Buttons of GUI
	  	buttonPanel.setLayout(new GridLayout(1,0));
	  	buttonPanel.add(setupButton);
	  	buttonPanel.add(playButton);
	  	buttonPanel.add(pauseButton);
	  	buttonPanel.add(tearButton);
	  	setupButton.addActionListener(new setupButtonListener());
	  	playButton.addActionListener(new playButtonListener());
	  	pauseButton.addActionListener(new pauseButtonListener());
	  	tearButton.addActionListener(new tearButtonListener());

	  	//Image display label
	  	iconLabel.setIcon(null);

	  	//frame layout
	  	mainPanel.setLayout(null);
	  	mainPanel.add(iconLabel);
	  	mainPanel.add(buttonPanel);
	  	iconLabel.setBounds(0,0,380,280);
	  	buttonPanel.setBounds(0,280,380,50);

	  	f.getContentPane().add(mainPanel, BorderLayout.CENTER);
	  	f.setSize(new Dimension(390,370));
	  	f.setVisible(true);

	  	//init timer
	  	timer = new Timer(20, new timerListener());
	  	timer.setInitialDelay(0);
	  	timer.setCoalesce(true);

	  	//Buffer length for receving data
	  	buffer = new byte[15000];
  	}
  //main
  public static void main(String argv[]) throws Exception
  {
    //Create a Client object
	int port = readPortFromFile("src/portToUseFile.txt");
	writePortToFile(++port,"src/portToUseFile.txt");
    Client theClient = new Client();
    // Server IP address
    String ServerHost = "127.0.0.1";
    InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);
    // Media File location
    video_filename = "media/movie.Mjpeg" ;
    theClient.RTSP_socket = new Socket(ServerIPAddr, port);

    //Set buffer for input output stream
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSP_socket.getInputStream()) );
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSP_socket.getOutputStream()) );
    state = INIT;
  }
  //Handler for Setup button

    class setupButtonListener implements ActionListener{
    public void actionPerformed(ActionEvent e){
    if (state == INIT)
	{
	  try{
	      //Read and write operation for the socket created
		  RTP_RECV_PORT = readPortFromFile("src/ReciverPort.txt");
		writePortToFile(++RTP_RECV_PORT,"src/ReciverPort.txt");
	    RTPsocket = new DatagramSocket(RTP_RECV_PORT);
	    RTPsocket.setSoTimeout(5);
	  }
	  catch (SocketException se)
	    {
	      System.out.println("Socket exception: "+se);
	      se.printStackTrace();
	      System.exit(0);
	    }

	  //Initalize the RTP packet Sequence Number
	  RTSP_sequence_num = 1;

	  //Send SETUP message to the server from client
	  send_RTSP_request("SETUP");

	  //Wait for the response
	  if (parse_server_response() != 200)
	    System.out.println("Invalid Server Response");
	  else
	    {
	      //change RTSP state and print it on the console
	      state = READY;
	      System.out.println("New RTSP state: READY");
	    }
	}
    }
  }

  //Handler for Play button
  class playButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
      if (state == READY)
	{

	  RTSP_sequence_num++;

	  //PLAY message to the server
	  send_RTSP_request("PLAY");

	  //Wait for the response from server
	  if (parse_server_response() != 200)
		  System.out.println("Invalid Server Response");
	  else
	    {
	      state = PLAYING;
	      System.out.println("New RTSP state: PLAYING");
	      timer.start();
	    }
	}
    }
  }

  //Handler for Pause button
  class pauseButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
      if (state == PLAYING)
	{
	  send_RTSP_request("PAUSE");
	 if (parse_server_response() != 200)
		  System.out.println("Invalid Server Response");
	  else
	    {
	      //change RTSP state and print out new state
	      state = READY;
	      System.out.println("New RTSP state: READY");

	      //stop the timer
	      timer.stop();
	    }
	}
    }
  }

  //Handler for Teardown button
  class tearButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
      send_RTSP_request("TEARDOWN");

      //Wait for the response
      if (parse_server_response() != 200)
	System.out.println("Invalid Server Response");
      else
	{
	  timer.stop();
	  System.exit(0);
	}
    }
  }
  class timerListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      recv_dp = new DatagramPacket(buffer, buffer.length);

      try{
	RTPsocket.receive(recv_dp);
	RTPpacket rtp_packet = new RTPpacket(recv_dp.getData(), recv_dp.getLength());
	//prints the message from the server once the packet is received
	System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());
	rtp_packet.printheader();
	int payload_length = rtp_packet.getpayload_length();
	byte [] payload = new byte[payload_length];
	rtp_packet.getpayload(payload);
	Toolkit toolkit = Toolkit.getDefaultToolkit();
	Image image = toolkit.createImage(payload, 0, payload_length);

	//Display the image on the GUI console
	icon = new ImageIcon(image);
	iconLabel.setIcon(icon);
      }
      catch (InterruptedIOException iioe){
      }
      catch (IOException ioe) {
	System.out.println("Exception caught: "+ioe);
      }
    }
  }
  //Parse Server Response
  private int parse_server_response()
  {
    int reply_code = 0;

    try{
      //parse status line and extract the reply_code:
      String StatusLine = RTSPBufferedReader.readLine();
      System.out.println(StatusLine);

      StringTokenizer tokens = new StringTokenizer(StatusLine);
      tokens.nextToken();
      reply_code = Integer.parseInt(tokens.nextToken());
      if (reply_code == 200)
	{
	  String SeqNumLine = RTSPBufferedReader.readLine();
	  System.out.println(SeqNumLine);
    String SessionLine = RTSPBufferedReader.readLine();
	  System.out.println(SessionLine);
	  tokens = new StringTokenizer(SessionLine);
	  tokens.nextToken();
	  RTSP_id = Integer.parseInt(tokens.nextToken());
	}
    }
    catch(Exception ex)
      {
	System.out.println("Exception caught : "+ex);
	System.exit(0);
      }

    return(reply_code);
  }
  //RTSP request type for server
  private void send_RTSP_request(String request_type)  //request_type=SETUP/PLAY/TEARDOWN......
	{
		try {
			RTSPBufferedWriter.write(request_type + " " + video_filename
					+ " RTSP/1.0" + CRLF);
			RTSPBufferedWriter.write("CSeq: " + RTSP_sequence_num + CRLF);
			//condition when the request type
			if ((new String(request_type)).compareTo("SETUP") == 0)
				RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= "
						+ RTP_RECV_PORT + CRLF);
			else
				RTSPBufferedWriter.write("Session: " + RTSP_id + "\n");

			RTSPBufferedWriter.flush();
		} catch (Exception ex) {
			System.out.println("Exception caught : " + ex);
			System.exit(0);
		}
	}
  public static void writePortToFile(int port,String fileName)
  {
	 try {
		 PrintStream ps = new PrintStream(new FileOutputStream(new File(fileName)));
		ps.print(port);
		ps.flush();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
  }
  public static int readPortFromFile(String fileName)
  {
	  int port = 0;
	  try {
		FileReader fis = new FileReader(new File(fileName));
		BufferedReader br = new BufferedReader(fis);
		do{
		port =  Integer.parseInt(br.readLine());
		if(port!= -1)
			return port;
		}while(port!=-1);
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}catch(Exception e)
	{
		e.printStackTrace();
	}
	return port;
  }
}
