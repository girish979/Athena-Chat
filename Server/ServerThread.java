
/****************************************************
 * Athena: Encrypted Messaging Application v.0.0.2
 * By: 	
 * 			Gregory LeBlanc
 * 			Norm Maclennan 
 * 			Stephen Failla
 * 
 * This program allows a user to send encrypted messages over a fully standardized messaging architecture. It uses RSA with (x) bit keys and SHA-256 to 
 * hash the keys on the server side. It also supports fully encrypted emails using a standardized email address. The user can also send "one-off" emails
 * using a randomly generated email address
 * 
 * File: ServerThread.java
 * 
 * Each connection from a client gets its own thread. User logs in, thread handles sending messages to other users.
 *
 * Sender's thread handles sending to recipient's socket.
 *
 * Thread's life is governed by an int isAlive. Set to 1 in the constructor, and set to 0 when user is likey disconnected.
 *
 ****************************************************/

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Enumeration;
import java.io.*;
import java.net.*;

//TODO: Do we need JOptionPane for server?! It's not used anywhere else.
import javax.swing.JOptionPane;

//TODO: Do we really need this? It does nothing ATM.
import com.sun.org.apache.xpath.internal.FoundIndex;

public class ServerThread extends Thread
{
	//Change to 1 for debug output
	private int debug = 1;

	//Create the DataInputStream on the current socket 
	public DataInputStream din = null;
	public DataOutputStream dout = null;

	// The Server that created this thread
	private static Server server;

	//Define Global Variable Username / Password
	private String username;
	private String password;

	//Our current socket
	public Socket socket;

	//Message digest for the hashed password
	MessageDigest hashedPassword;
	//Governs thread life. If connection is not alive, thread terminates
	private int isAlive=1;

	// Constructor. Instantiate this thread on the current socket
	public ServerThread( Server server, Socket socket ) {

		// Remember which socket we are on
		this.server = server;
		this.socket = socket;

		//Start up the thread
		start();
	}

	//This runs when the thread starts. It controls everything.
	public void run() {
		try {
			//Create a datainputstream on the current socket to accept data from the client
			din = new DataInputStream( socket.getInputStream() );

			//Getting the Username and Password over the stream for authentication
			username = din.readUTF(); 
			if(username.equals("Interupt")) { 
				
			} else { 
			password = din.readUTF(); 
			System.out.println("PASSWORD: " + password);
			//Ya'll like some hash?
			String hashedPassword = byteArrayToHexString(computeHash(password));
			System.out.println("DHFAHFSHFFA " + hashedPassword);

			//Debug statements
			if (debug==1)System.out.println("Username: " + username);
			if (debug==1)System.out.println("Password: " + password);

			//Authenticate the user.
			String loginOutcome = login(username, hashedPassword);
			if (debug==1)System.out.println(loginOutcome);

			//Maps username to socket after user logs in
			server.mapUserSocket(username, socket);	
			}
			if(username.equals("Interupt")) {
				routeMessage(din);
				server.removeConnection(socket);				
			} else { 
			//Route around messages coming in from the client while they are connected
			while (isAlive==1) {
				//Take in messages from this thread's client and route them to another client
				routeMessage(din);
				}
			}

		} catch ( EOFException ie ) {
		} catch ( IOException ie ) {
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			//Socket is closed, remove it from the list
			server.removeConnection( socket, username );
		}
	}

	//Takes in a recipient and message from this thread's user
	//and routes the message to the recipient.
	public void routeMessage(DataInputStream din){
		try {
			String toUser=din.readUTF();
			String message=din.readUTF();

			//Is the message an eventcode meant for the server?
			if (toUser.equals("Aegis")) { 
				if(debug==1)System.out.println("Server eventcode detected!");
				systemMessageListener(Integer.parseInt(message));
				return;
			}else { 
				if(debug==1)System.out.println("Routing normal message");
				sendMessage(toUser, username, message);
			}

		} catch (IOException e) {isAlive=0;}
	}

	//Method that handles client to server messages
	public void systemMessageListener(int eventCode) {

		switch(eventCode) { 
		case 000: createUsername();
		break;
		case 001: negotiateClientStatus();
		System.out.println("Event code received. negotiateClientStatus() run.");
		break;
		case 002: server.sendToAll("ServerLogOn", username);
		break;
		default: return;
		}
	}

	public void negotiateClientStatus() {
		try { 
			//Acknowledge connection. Make sure we are doing the right thing
			sendSystemMessage(username, "Access granted. Send me the username.");
			//Listen for the username
			String findUser = din.readUTF();
			//Print out the received username
			System.out.println("Username received: " + findUser);
			//Check to see if the username is in the current Hashtable, return result
			//TODO: Do we need to sleep?
			Thread.sleep(50);
			if ((server.userToSocket.containsKey(findUser))) { 
				sendSystemMessage(username,"1");
				System.out.println("(Online)\n");
			} else { sendSystemMessage(username,"0");
			System.out.println("(Offline)\n");
			} 
		} catch ( java.io.IOException e ) { }
		catch (InterruptedException ie) { } 
	}

	//TODO Make this work better.
	public boolean createUsername() { 
		try { 
			//Use dbConnect() to connect to the database
			Connection con = server.dbConnect();

			//Create a statement and resultset for the query
			Statement stmt;
			Statement insertSTMT;
			ResultSet rs = null; 

			//Disregard two messages, the two others are the username and password
			String firstName = din.readUTF();
			String lastName = din.readUTF();
			String emailAddress = din.readUTF();
			String newUser = din.readUTF();
			String newPassword = din.readUTF();

			//Ya'll like some hash?
			String hashedPassword = byteArrayToHexString(ServerThread.computeHash(newPassword));
			System.out.println("HASHEDMOFO: " + hashedPassword);


			stmt = con.createStatement();
			if(debug==1)System.out.println("Statement created\nCreating username: "+newUser+"\nPassword: "+ hashedPassword);

			//See if the username already exists.
			rs = stmt.executeQuery("SELECT * FROM Users WHERE username = '" + newUser+"'");
			if(debug==1)System.out.println("newUser: " + newUser);

			//Test to see if there are any results
			if (rs.next()) { 
				//sendMessage(username,"Aegis","Username ("+newUser+")  has already been taken");
				return false;
			}
			else { 
				//Grab the users new password
				String insertString = "insert into Users (FirstName, LastName, EmailAddress, username, password) values('" + firstName + "', '" + lastName + "', '" + emailAddress + "', '" + newUser + "', '" + hashedPassword + "')";
				insertSTMT = con.createStatement();
				insertSTMT.executeUpdate(insertString);

				//Close Connections
				stmt.close();
				insertSTMT.close();
				con.close();

				//Inform of our success
				//sendMessage(username, "Aegis", "User created succesfully.");
				server.updateHashTable();
				return true;
			}
		}catch (SQLException se) { 
			System.out.print(se.toString());
			return false;
		}catch (IOException ie) { 
			System.out.println(ie.toString());
			return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}


	//Sends message message from user fromUser (this thread/socket) to user toUser (another socket)
	//TODO: Separate findOuputSteam from this method?
	void sendMessage(String toUser, String fromUser, String message) {
		Socket foundSocket = null;

		//Debug statement: who is this going to?
		if(debug==1)System.out.print(toUser);

		//Look up the socket associated with the with whom we want to talk
		//We will use this to find which outputstream to send out
		//If we cannot find the user or socket, send back an error
		if ((server.userToSocket.containsKey(toUser))) { 
			if(debug==1)System.out.print("Found user.. Continuing...");
			foundSocket = (Socket) server.userToSocket.get(toUser);
			if(debug==1)System.out.print("Found Socket: " + foundSocket);
		} else { sendMessage(fromUser, "UnavailableUser", toUser); return; } 

		//Find the outputstream associated with toUser's socket
		//We send data through this outputstream to send the message
		//If we cannot find the outputstream, send back an error
		//This should not fail
		if (server.outputStreams.containsKey(foundSocket)) { 
			dout = (DataOutputStream) server.outputStreams.get(foundSocket);
		} else { sendMessage(fromUser, "MissingSocket", toUser); return; }

		//Send the message, and the user it is from
		try {
			dout.writeUTF(fromUser);
			dout.writeUTF(message);
		} catch( IOException ie ) { System.out.println( ie ); }
	}

	//Send system Messages to selected user
	void sendSystemMessage(String toUser, String message) { 
		Socket foundSocket = null;

		//Debug statement: who is this going to?
		if(debug==1)System.out.println("Who is this message going to? " + toUser);

		//Look up the socket associated with the with whom we want to talk
		//We will use this to find which outputstream to send out
		//If we cannot find the user or socket, send back an error
		if ((server.userToSocket.containsKey(toUser))) { 
			if(debug==1)System.out.println("Found user.. Continuing...");
			foundSocket = (Socket) server.userToSocket.get(toUser);
			if(debug==1)System.out.println("Found Socket: " + foundSocket);
		} 

		//Find the outputstream associated with toUser's socket
		//We send data through this outputstream to send the message
		//If we cannot find the outputstream, send back an error
		//This should not fail
		if (server.outputStreams.containsKey(foundSocket)) { 
			dout = (DataOutputStream) server.outputStreams.get(foundSocket);
		} 

		//Send the message, and the user it is from
		try {
			//dout.writeUTF(toUser);
			dout.writeUTF(message);
			System.out.println("Message sent:\n " + message);
		} catch( IOException ie ) { System.out.println( ie ); }
	}
	//This will authenticate the user, before they are allowed to send messages.	
	public String login (String clientName, String clientPassword) { 

		//Get the password from the hashtable
		String hashedPassword = server.authentication.get(clientName).toString();

		//Debug messages.
		//TODO: Come up with better debug messages
		if (debug==1)System.out.println("User logging in...");
		if (debug==1)System.out.println("Hashed Password:" + hashedPassword);
		if (debug==1)System.out.println("Username :" + clientName);

		//Verify the password hash provided from the user matches the one in the server's hashtable
		if (clientPassword.equals(hashedPassword)) { 
			//Run some command that lets user log in!
			//TODO: We need to broadcast a message letting everyone know a user logged in?
			String returnMessage = "You're logged in!!!!"; //Depreciated - See next line
			return returnMessage;
		}else { 
			//Login fail handler
			server.removeConnection(socket, clientName);
			return "Login Failed";  
		}	
	}
	//This will return the hashed input string
	public static byte[] computeHash(String toHash) throws Exception { 
		MessageDigest d = null;
		d = MessageDigest.getInstance("SHA-1");
		d.reset();
		d.update(toHash.getBytes());
		return d.digest();	
	}

	//This will turn a byteArray to a String
	public static String byteArrayToHexString(byte[] b) { 
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) { 
			int v = b[i] & 0xff;
			if (v < 16) { 
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}
}

