package com.cn2.communication;
import javax.sound.sampled.*; 
import java.util.Calendar; // MINE
import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;//MINE
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {
	
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;				
	static JButton closeCallButton;
	static JButton deafenButton;
	static JButton muteButton;
	static boolean close;
	static boolean deafen;
	static boolean mute;
	static String friend;
	static int getMessPort;
	static int getCallPort;
	static int sendMessPort;
	static int sendCallPort;
	
	public App(String title) {
		

		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");
		closeCallButton = new JButton("Close call");
		deafenButton = new JButton("Deafen");
		muteButton = new JButton("Mute");
		
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(closeCallButton);
		add(deafenButton);
		add(muteButton);
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		closeCallButton.addActionListener(this);
		deafenButton.addActionListener(this);
		muteButton.addActionListener(this);
		
		
	}
	

	public static void main(String[] args) {
		

		App app = new App("****OUR CHAT****");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  
		
		friend = JOptionPane.showInputDialog("Enter the IP of your friend: ");
		//PORTS:
		sendMessPort = Integer.parseInt(JOptionPane.showInputDialog("Choose message destination port: "));
		getMessPort = Integer.parseInt(JOptionPane.showInputDialog("Choose your message receiving port: "));
		sendCallPort = Integer.parseInt(JOptionPane.showInputDialog("Choose sound destination port: "));
		getCallPort = Integer.parseInt(JOptionPane.showInputDialog("Choose your sound receiving port: "));
		
		
		startReceivingMessages();
		
	}
	
	
	static public void startSendingMessages() {
		new Thread(() -> {
			String message = inputTextField.getText();
			if(!message.isEmpty()) {
				try {
					DatagramSocket sendSocket = new DatagramSocket(); //socket for sending 
					byte[] buffer =  message.getBytes(); //reverse function from receiving socket
					InetAddress recipientAddress = InetAddress.getByName(friend);
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, recipientAddress, sendMessPort);
					
					Calendar currentTime = Calendar.getInstance();
					sendSocket.send(packet);
					sendSocket.close();
					inputTextField.setText("");
					textArea.append("Message sent: " + message + "\t" + "@: " + currentTime.getTime() + "\n");
					
				} catch (IOException ex) {
					
					ex.printStackTrace();
					System.out.println("Problem with message sending port");
				} 
		}
	}).start();
	}
	
	static public void startReceivingMessages() {
		
		new Thread(() -> {
			
			try {
				DatagramSocket socket = new DatagramSocket(getMessPort); // socket for receiving messages
				byte[] buffer = new byte[500]; //make a buffer with 500 bytes capacity for incoming messages
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // packet === buffer content (inc message) 
			
				while(true) {
					socket.receive(packet);
					String message = new String(packet.getData(), 0, packet.getLength());
					Calendar currentTime = Calendar.getInstance();
					textArea.append("Message received: " + message + "\t" + "@: " + currentTime.getTime() + "\n");
				}
		 
		}catch(IOException e) {
			e.printStackTrace();
			System.out.println("Problem with message receiving port");
		}
		
	}).start();
	}
	
	static public void outgoingSound() {
		new Thread(() -> {
			try {
				AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
				TargetDataLine mic = AudioSystem.getTargetDataLine(format); 
				mic.open(format);
				mic.start();
				
				DatagramSocket socket = new DatagramSocket(); // socket for sending sound
				byte[] sendBuffer = new byte[1024]; //adjust for better quality
				InetAddress recepientIp =  InetAddress.getByName(friend);
				//DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length);
				//byte[] outgoingSound = sendPacket.getData();
					
				
				while(!mute) {
						int bytesInBuffer = mic.read(sendBuffer, 0, sendBuffer.length);
						DatagramPacket sendPacket = new DatagramPacket(sendBuffer, bytesInBuffer, recepientIp, sendCallPort);
						socket.send(sendPacket);
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Problem with sound receiving port");
			} catch (LineUnavailableException e) {
				e.printStackTrace();
				System.out.println("Line unavailable");
			}
		}).start();
	}
	
	static public void incomingSound() {
		new Thread(() -> {
			try {
				AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
				SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
				speaker.open(format);
				speaker.start();
				
				DatagramSocket receiveSocket = new DatagramSocket(getCallPort); //receive sound
				byte[] receiveBuffer = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);//link buffer to packet , packet wraps buffer
				
				while(!deafen) {
					receiveSocket.receive(receivePacket);//copy data from packet (buffer is inside packet)
					speaker.write(receivePacket.getData(), 0, receivePacket.getLength());
					
				}
				
			}  catch (LineUnavailableException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}).start();	
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
	
		if (e.getSource() == sendButton){
			
			startSendingMessages();
				
			}
		else if(e.getSource() == callButton){
			deafen = false;
			mute = false;
			outgoingSound();
			incomingSound();
		}
		if(e.getSource() == closeCallButton) {
			deafen = true;
			mute = true;
		}
		if(e.getSource() == deafenButton) {
			deafen = true;
		}
		if(e.getSource() == muteButton) {
			mute = true;
		}

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
	
}
