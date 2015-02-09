package org.zu.ardulink.connection.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import org.zu.ardulink.Link;
import org.zu.ardulink.connection.Connection;
import org.zu.ardulink.connection.ConnectionContact;

/**
 * [ardulinktitle] [ardulinkversion]
 * Used to simplify communication. One connection per instance of this class can be handled.
 * 
 * A separate {@link Thread} is started to handle messages that are being
 * received over the Serial interface.
 * 
 * This class also makes packages out of a stream of bytes received, using a
 * {@link #divider}, and sending these packages as an array of <b>int</b>s (each
 * between 0 and 255) to a function implemented by a class implementing the
 * {@link org.zu.ardulink.connection.ConnectionContact}-interface.
 * 
 * [adsense]
 */
public abstract class AbstractSerialConnection implements Connection {
	
	public static final String DEFAULT_CONNECTION_NAME = Link.DEFAULT_LINK_NAME;
	
	private InputStream inputStream;
	private OutputStream outputStream;

	/**
	 * The Thread used to receive the data from the Serial interface.
	 */
	private Thread reader;

	/**
	 * The status of the connection.
	 */
	private boolean connected = false;

	/**
	 * Communicating between threads, showing the {@link #reader} when the
	 * connection has been closed, so it can {@link Thread#join()}.
	 */
	private boolean end = true;

	/**
	 * Link to the instance of the class implementing {@link org.zu.ardulink.connection.ConnectionContact}.
	 */
	private ConnectionContact contact;
	
	/**
	 * A small <b>int</b> representing the number to be used to distinguish
	 * between two consecutive packages. It can only take a value between 0 and
	 * 255. Note that data is only sent to
	 * {@link org.zu.ardulink.connection.ConnectionContact#parseInput(int, int, int[])} once the following
	 * 'divider' could be identified.
	 * 
	 * As a default, <b>255</b> is used as a divider (unless specified otherwise
	 * in the constructor).
	 * 
	 * @see org.zu.ardulink.connection.serial.AbstractSerialConnection#SerialConnection(int, ConnectionContact, int)
	 */
	private int divider;
	
	/**
	 * <b>String</b> identifying the specific instance of the SerialConnection-class. While
	 * having only a single instance, 'id' is irrelevant. However, having more
	 * than one open connection (using more than one instance of {@link AbstractSerialConnection}
	 * ), 'id' helps identifying which Serial connection a message or a log
	 * entry came from.
	 */
	private String id;

	private int[] tempBytes;
	int numTempBytes = 0, numTotBytes = 0;

	/**
	 * @param id
	 *            <b>int</b> identifying the specific instance of the
	 *            SerialConnection-class. While having only a single instance,
	 *            {@link #id} is irrelevant. However, having more than one open
	 *            connection (using more than one instance of SerialConnection),
	 *            {@link #id} helps identifying which Serial connection a
	 *            message or a log entry came from.
	 * 
	 * @param contact
	 *            Link to the instance of the class implementing
	 *            {@link org.zu.ardulink.connection.ConnectionContact}.
	 * 
	 * @param divider
	 *            A small <b>int</b> representing the number to be used to
	 *            distinguish between two consecutive packages. It can take a
	 *            value between 0 and 255. Note that data is only sent to
	 *            {@link org.zu.ardulink.connection.ConnectionContact#parseInput(int, int, int[])} once the
	 *            following {@link #divider} could be identified.
	 */
	public AbstractSerialConnection(String id, ConnectionContact contact, int divider) {
		this.contact = contact;
		this.divider = divider;
		if (this.divider > 255)
			this.divider = 255;
		if (this.divider < 0)
			this.divider = 0;
		this.id = id;
		tempBytes = new int[1024];
	}

	/**
	 * Just as {@link #AbstractSerialConnection(int, ConnectionContact, int)}, but with a default
	 * {@link #divider} of <b>255</b>.
	 * 
	 * @see #SerialConnection(int, ConnectionContact, int)
	 */
	public AbstractSerialConnection(String id, ConnectionContact contact) {
		this(id, contact, 255);
	}

	/**
	 * Just as {@link #AbstractSerialConnection(int, ConnectionContact, int)}, but with a default
	 * {@link #divider} of <b>255</b> and a default {@link #id} of DEFAULT_CONNECTION_NAME. This
	 * constructor may mainly be used if only one Serial connection is needed at
	 * any time.
	 * 
	 * @see #SerialConnection(int, ConnectionContact, int)
	 */
	public AbstractSerialConnection(ConnectionContact contact) {
		this(DEFAULT_CONNECTION_NAME, contact);
	}

	/**
	 * Just as (@link {@link #AbstractSerialConnection(ConnectionContact)}, but with null contact.
	 * The first method to use should be {@link #setConnectionContact(ConnectionContact)}
	 */
	public AbstractSerialConnection() {
		this(null);
	}

	/**
	 * This method is used to get a list of all the available Serial ports
	 * (note: only Serial ports are considered). Any one of the elements
	 * contained in the returned {@link Vector} can be used as a parameter in
	 * {@link #connect(String)} or {@link #connect(String, int)} to open a
	 * Serial connection.
	 * 
	 * @return A {@link Vector} containing {@link String}s showing all available
	 *         Serial ports.
	 *         
	 * Luciano Zu has modified return type from Vector to List
	 */
	public abstract List<String> getPortList();

	/**
	 * A separate class to use as the {@link org.zu.ardulink.connection.serial.AbstractSerialConnection#reader}. It is run as a
	 * separate {@link Thread} and manages the incoming data, packaging them
	 * using {@link org.zu.ardulink.connection.serial.AbstractSerialConnection#divider} into arrays of <b>int</b>s and
	 * forwarding them using
	 * {@link org.zu.ardulink.connection.ConnectionContact#parseInput(int, int, int[])}.
	 * 
	 */
	private class SerialReader implements Runnable {
		InputStream in;

		public SerialReader(InputStream in) {
			this.in = in;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int len = -1, i, temp;
			try {
				while (!end) {
					// if ((in.available()) > 0) {
						if ((len = this.in.read(buffer)) > -1) {
							for (i = 0; i < len; i++) {
								temp = buffer[i];
								 // adjust from C-Byte to Java-Byte
								if (temp < 0)
									temp += 256;
								if (temp == divider) {
									if  (numTempBytes > 0) {
										contact.parseInput(id, numTempBytes,
												tempBytes);
									}
									numTempBytes = 0;
								} else {
									tempBytes[numTempBytes] = temp;
									++numTempBytes;
								}
							}
						}
					// }
				}
			} catch (IOException e) {
				end = true;
				try {
					outputStream.close();
					inputStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				connected = false;
				contact.disconnected(id);
				contact.writeLog(id, "connection has been interrupted");
			}
		}
	}
	
	public void startReader() {
		if(end) {
			reader = (new Thread(new SerialReader(inputStream)));
			end = false;
			reader.start();
		}
	}

	public void stopReader() {
		end = true;
//		try {
//			outputStream.close();
//			inputStream.close();
//			reader.join();
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * This method is included as a legacy. Depending on the other side of the
	 * Serial port, it might be easier to send using a String. Note: this method
	 * does not add the {@link #divider} to the end.
	 * 
	 * If a connection is open, a {@link String} can be sent over the Serial
	 * port using this function. If no connection is available, <b>false</b> is
	 * returned and a message is sent using
	 * {@link org.zu.ardulink.connection.ConnectionContact#writeLog(int, String)}.
	 * 
	 * @param message
	 *            The {@link String} to be sent over the Serial connection.
	 * @return <b>true</b> if the message could be sent, <b>false</b> otherwise.
	 */
	public boolean writeSerial(String message) {
		boolean success = false;
		if (isConnected()) {
			try {
				outputStream.write(message.getBytes());
				outputStream.flush();
				success = true;
			} catch (IOException e) {
				disconnect();
			}
		} else {
			contact.writeLog(id, "No port is connected.");
		}
		return success;
	}

	/**
	 * If a connection is open, an <b>int</b> between 0 and 255 (except the
	 * {@link org.zu.ardulink.connection.serial.AbstractSerialConnection#divider}) can be sent over the Serial port using this
	 * function. The message will be finished by sending the
	 * {@link org.zu.ardulink.connection.serial.AbstractSerialConnection#divider}. If no connection is available, <b>false</b>
	 * is returned and a message is sent using
	 * {@link org.zu.ardulink.connection.ConnectionContact#writeLog(int, String)}.
	 * 
	 * @param numBytes
	 *            The number of bytes to send over the Serial port.
	 * @param message
	 *            [] The array of<b>int</b>s to be sent over the Serial
	 *            connection (between 0 and 256).
	 * @return <b>true</b> if the message could be sent, <b>false</b> otherwise
	 *         or if one of the numbers is equal to the #{@link AbstractSerialConnection#divider}
	 *         .
	 */
	public boolean writeSerial(int numBytes, int message[]) {
		boolean success = true;
		int i;
		for (i = 0; i < numBytes; ++i) {
			if (message[i] == divider) {
				success = false;
				break;
			}
		}
		if (success && isConnected()) {
			try {
				for (i = 0; i < numBytes; ++i) {
						outputStream.write(changeToByte(message[i]));
				}
				outputStream.write(changeToByte(divider));
				outputStream.flush();
			} catch (IOException e) {
				success = false;
				disconnect();
			}
		} else if (!success) {
			// message contains the divider
			contact.writeLog(id, "The message contains the divider.");
		} else {
			contact.writeLog(id, "No port is connected.");
		}
		return success;
	}

	private byte changeToByte(int num) {
		byte number;
		int temp;
		temp = num;
		if (temp > 255)
			temp = 255;
		if (temp < 0)
			temp = 0;
		number = (byte) temp;
		return number;
	}

	@Override
	public abstract boolean connect(Object... params);
	
	@Override
	public abstract boolean disconnect();

	@Override
	public void setConnectionContact(ConnectionContact connectionContact) {
		contact = connectionContact;
	}
	
	/**
	 * @return Whether this instance of {@link org.zu.ardulink.connection.serial.AbstractSerialConnection} has currently an
	 *         open connection of not.
	 */
	public boolean isConnected() {
		return connected;
	}

	protected void setConnected(boolean connected) {
		this.connected = connected;
	}

	public ConnectionContact getContact() {
		return contact;
	}

	public String getId() {
		return id;
	}

	public void writeLog(String text) {
		contact.writeLog(id, text);
	}

	protected void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	protected void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}
}
