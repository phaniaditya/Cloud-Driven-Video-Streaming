package com.project.client;

public class RTPpacket {
	// Here the size of the RTP header is initialized:
	static int HEADER_SIZE = 12;

	// Here are the fields that compose the RTP header
	public int Version;
	public int Padding;
	public int Extension;
	public int CC;
	public int Marker;
	public int PayloadType;
	public int SequenceNumber;
	public int TimeStamp;
	public int Ssrc;

	// RTP header bitstream
	public byte[] header;

	// Here the size of the RTP payload is declared
	public int payload_size;
	// Bitstream of the RTP payload is declared
	public byte[] payload;

	// Here the Constructor of an RTPpacket object from header fields and payload
	// bitstream is created
	public RTPpacket(int PType, int Framenumber, int Time, byte[] data,
			int data_length) {
		// Default header fields are filled:
		Version = 2;
		Padding = 0;
		Extension = 0;
		CC = 0;
		Marker = 0;
		Ssrc = 0;

		// Filling the changing header fields:
		SequenceNumber = Framenumber;
		TimeStamp = Time;
		PayloadType = PType;

		// build the header bistream
	
		header = new byte[HEADER_SIZE];

		header[1] = (byte) ((Marker << 7) | PayloadType);

		header[2] = (byte) (SequenceNumber >> 8);
		header[3] = (byte) (SequenceNumber);

		for (int i = 0; i < 4; i++)
			header[7 - i] = (byte) (TimeStamp >> (8 * i));

		for (int i = 0; i < 4; i++)
			header[11 - i] = (byte) (Ssrc >> (8 * i));

		payload_size = data_length;
		payload = new byte[data_length];

		payload = data;

		 
	}

	// Constructor of an RTPpacket object from the packet data
	public RTPpacket(byte[] packet, int packet_size) {
		// filling the default fields:
		Version = 2;
		Padding = 0;
		Extension = 0;
		CC = 0;
		Marker = 0;
		Ssrc = 0;

		// Here we are checking if total packet size is lower than the header size
		if (packet_size >= HEADER_SIZE) {
			//The header bitsream is loaded to header:
			header = new byte[HEADER_SIZE];
			for (int i = 0; i < HEADER_SIZE; i++)
				header[i] = packet[i];

			// get the payload bitstream:
			payload_size = packet_size - HEADER_SIZE;
			payload = new byte[payload_size];
			for (int i = HEADER_SIZE; i < packet_size; i++)
				payload[i - HEADER_SIZE] = packet[i];

			// Here the changing fields of the header are interpret:
			PayloadType = header[1] & 127;
			SequenceNumber = unsigned_int(header[3]) + 256
					* unsigned_int(header[2]);
			TimeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6])
					+ 65536 * unsigned_int(header[5]) + 16777216
					* unsigned_int(header[4]);
		}
	}

	// getpayload() returns the payload bitstream of the RTPpacket and its size
	public int getpayload(byte[] data) {

		for (int i = 0; i < payload_size; i++)
			data[i] = payload[i];

		return (payload_size);
	}

	// Here the length of the payload is returned by the getpayload_length() 
	public int getpayload_length() {
		return (payload_size);
	}

	// The total length of the RTP packet by getlength()
	public int getlength() {
		return (payload_size + HEADER_SIZE);
	}

	
	// getpacket() returns the packet data and its length
	
	public int getpacket(byte[] packet) {
		// The packet is constructed as packet = header + payload
		for (int i = 0; i < HEADER_SIZE; i++)
			packet[i] = header[i];
		for (int i = 0; i < payload_size; i++)
			packet[i + HEADER_SIZE] = payload[i];

		// This returns total size of the packet
		return (payload_size + HEADER_SIZE);
	}

	// gettimestamp

	public int gettimestamp() {
		return (TimeStamp);
	}

	// getsequencenumber
	public int getsequencenumber() {
		return (SequenceNumber);
	}

	// getpayloadtype
	public int getpayloadtype() {
		return (PayloadType);
	}

	// The headers are printed without the SSRC
	public void printheader() {

		for (int i = 0; i < (HEADER_SIZE - 4); i++) {
			for (int j = 7; j >= 0; j--)
				if (((1 << j) & header[i]) != 0)
					System.out.print("1");
				else
					System.out.print("0");
			System.out.print(" ");
		}

		System.out.println();
	}

	// The unsigned value of 8-bit integer number is returned
	static int unsigned_int(int number) {
		if (number >= 0)
			return (number);
		else
			return (256 + number);
	}

}
