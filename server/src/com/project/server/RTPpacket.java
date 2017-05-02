package com.project.server;
/* RTPpacket
   Team 16*/

public class RTPpacket{

  // The size of the RTP header is given as 12
  static int HEADER_SIZE = 12;

  //Declaring classes that compose the RTP header
  public int Version;
  public int Padding;
  public int Extension;
  public int CC;
  public int Marker;
  public int PayloadType;
  public int SequenceNumber;
  public int TimeStamp;
  public int Ssrc;
  
  // Declaring Bitstream of the RTP header
  public byte[] header;

  //Declaring size of the RTP payload
  public int payload_size;
  // Declaring Bitstream of the RTP payload
  public byte[] payload;
  
  //Constructor 
  public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length){
    
//The default header fields are set
    Version = 2;
    Padding = 0;
    Extension = 0;
    CC = 0;
    Marker = 0;
    Ssrc = 0;

    SequenceNumber = Framenb;
    TimeStamp = Time;
    PayloadType = PType;
    
    // header bistream:
    
    header = new byte[HEADER_SIZE];

    //The header array of byte with RTP header fields
    header[0] = new Integer((Version<<6)|(Padding<<5)|(Extension<<4)|CC).byteValue();
    header[1] = new Integer((Marker<<7)|PayloadType).byteValue();

    header[2] = new Integer(SequenceNumber>>8).byteValue();
    header[3] = new Integer(SequenceNumber).byteValue();

    for (int i=0; i < 4; i++)
	header[7-i] = new Integer(TimeStamp>>(8*i)).byteValue();
     
    for (int i=0; i < 4; i++)
	header[11-i] = new Integer(Ssrc>>(8*i)).byteValue();

    //payload bitstream:

    payload_size = data_length;
 System.out.println(payload_size);
 System.out.println("______________");
 System.out.println(data_length);
 System.out.println("+++++++++++++");
    payload = new byte[data_length];
    payload = data;
  }
    
  
  //Constructor 

  public RTPpacket(byte[] packet, int packet_size)
  {
    //The default fields are set
    Version = 2;
    Padding = 0;
    Extension = 0;
    CC = 0;
    Marker = 0;
    Ssrc = 0;

    //If the total packet size is lower than the header size then follow the below commands
  if (packet_size >= HEADER_SIZE) 
      {
	// header bitsream:
	header = new byte[HEADER_SIZE];
	for (int i=0; i < HEADER_SIZE; i++)
	  header[i] = packet[i];

	// payload bitstream:
	payload_size = packet_size - HEADER_SIZE;
	payload = new byte[payload_size];
	for (int i=HEADER_SIZE; i < packet_size; i++)
	  payload[i-HEADER_SIZE] = packet[i];

	//interpret the payload,sequence number and timestamp of the header :
	PayloadType = header[1] & 127;
	SequenceNumber = unsigned_int(header[3]) + 256*unsigned_int(header[2]);
	TimeStamp = unsigned_int(header[7]) + 256*unsigned_int(header[6]) + 65536*unsigned_int(header[5]) + 16777216*unsigned_int(header[4]);
      }
 }


  //The payload bistream of the RTPpacket and its size is returned
  
  public int getpayload(byte[] data) {

    for (int i=0; i < payload_size; i++)
      data[i] = payload[i];

    return(payload_size);
  }

  
  //The length of the payload is returned
  
  public int getpayload_length() {
    return(payload_size);
  }

  //The total length of the RTP packet is returned
  public int getlength() 
  {
    return(payload_size + HEADER_SIZE);
  }

  
  //The packet bitstream and its length is returned
  
  public int getpacket(byte[] packet)
  {
    
    for (int i=0; i < HEADER_SIZE; i++)
	packet[i] = header[i];
    for (int i=0; i < payload_size; i++)
	packet[i+HEADER_SIZE] = payload[i];

    //total size of the packet is returned
    return(payload_size + HEADER_SIZE);
  }

  //timestamp
  

 public int gettimestamp() {
    return(TimeStamp);
  }

  //sequencenumber
  public int getsequencenumber() {
    return(SequenceNumber);
  }

  //payloadtype
  
  public int getpayloadtype() {
    return(PayloadType);
  }


  
  //print headers without the SSRC
 
  public void printheader()
  {
    
    for (int i=0; i < (HEADER_SIZE-4); i++)
      {
	for (int j = 7; j>=0 ; j--)
	  if (((1<<j) & header[i] ) != 0)
	    System.out.print("1");
	else
	  System.out.print("0");
	System.out.print(" ");
      }

    System.out.println();
    
  }

  
  static int unsigned_int(int nb) {
    if (nb >= 0)
      return(nb);
    else
      return(256+nb);
  }
}

                                
