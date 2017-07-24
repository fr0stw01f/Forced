package me.zhenhao.forced.sharedclasses.networkconnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;
import me.zhenhao.forced.sharedclasses.SharedClassesSettings;
import me.zhenhao.forced.sharedclasses.util.NetworkSettings;


public class ServerCommunicator{

	private final Queue<ServerResponse> serverAnswers = new ConcurrentLinkedQueue<>();
	
	private final Object syncToken;
	
	public ServerCommunicator(Object syncToken) {
		this.syncToken = syncToken;
	}
	
	
	public ServerResponse getResultForRequest(IClientRequest request) {
		Log.i(SharedClassesSettings.TAG, "Getting request for" + request + "...");

		synchronized (syncToken) {
			GettingTask client = new GettingTask(request);
			Thread thread = new Thread(client);
			thread.start();
			try {
				syncToken.wait();
				return serverAnswers.poll();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}	
		}
	}
	
	
	public void send(final Collection<IClientRequest> request, final boolean waitForResponse) {
		Log.i(SharedClassesSettings.TAG, "Server communicator sending request...");

		Thread thread = new Thread(new SendingTask(request, waitForResponse));
		thread.start();

		// Wait for completion if we have to
		try {
			if (syncToken != null && waitForResponse)
				synchronized (syncToken) {
					syncToken.wait();
				}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	private class GettingTask implements Runnable {

		private final IClientRequest request;
		
		GettingTask(IClientRequest request) {
			this.request = request;
		}
		
		
		@Override
		public void run() {
			if(request == null)
				throw new RuntimeException("Server-Request should not be null!");
			ObjectOutputStream oos;
			ObjectInputStream ois;
			try {
				Log.i(SharedClassesSettings.TAG,
					"Establishing socket connection to " + NetworkSettings.SERVER_IP + " " +
							NetworkSettings.SERVERPORT_OBJECT_TRANSFER);
				try (Socket socket = new Socket(NetworkSettings.SERVER_IP, NetworkSettings.SERVERPORT_OBJECT_TRANSFER)) {
					if (!socket.isConnected()) {
						socket.close();
						throw new RuntimeException("Socket is not established");
					}

					// Create our streams
					oos = new ObjectOutputStream(socket.getOutputStream());
					ois = new ObjectInputStream(socket.getInputStream());

					// Send the request
					Log.i(SharedClassesSettings.TAG, "Sending single event to server...");
					sendRequest(oos);

					// Wait for the response
					Log.i(SharedClassesSettings.TAG, "Waiting for single server response...");

					ServerResponse response = getResponse(ois);

					// Even in case the connection dies, take what we have and run.
					Log.i(SharedClassesSettings.TAG, "OK, done.");
					Log.i(SharedClassesSettings.TAG, response.toString());
					serverAnswers.add(response);

					// Tell the server that we're ready to close the connection
					Log.i(SharedClassesSettings.TAG, "All objects confirmed, closing connection...");
					oos.writeObject(new CloseConnectionRequest());
					oos.flush();
					socket.shutdownOutput();

					// Wait for the server to acknowledge that it's going away
					// Make sure that the server isn't already dead as a doornail
					ois.mark(1);
					if (ois.read() != -1) {
						ois.reset();

						Log.i(SharedClassesSettings.TAG, "Waiting for server shutdown confirmation...");
						ois.readObject();

						// We close the socket anyway
						// if (socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown())
						// socket.shutdownInput();
					}
				}
			}
			catch (IOException | ClassNotFoundException e) {			
				e.printStackTrace();
			}
			
			if (syncToken != null) {
				synchronized(syncToken) {
					syncToken.notify();
				}
			}
			
			Log.i(SharedClassesSettings.TAG, "End of CLIENT thread.");
		}
		
		
		private void sendRequest(ObjectOutputStream out) throws IOException {
			out.writeObject(request);
		}
		
		
		private ServerResponse getResponse(ObjectInputStream input) throws ClassNotFoundException, IOException {
			return (ServerResponse)input.readObject();
		}
	}

	private class SendingTask implements Runnable {
		final Collection<IClientRequest> request;
		final boolean waitForResponse;

		SendingTask(final Collection<IClientRequest> request, final boolean waitForResponse) {
			this.request = request;
			this.waitForResponse = waitForResponse;
		}

		@Override
		public void run() {
			// No need to send empty requests
			if (request.isEmpty())
				return;

			ObjectOutputStream oos;
			ObjectInputStream ois;
			try {
				Log.i(SharedClassesSettings.TAG,
						"Establishing socket connection to " + NetworkSettings.SERVER_IP + " " +
								NetworkSettings.SERVERPORT_OBJECT_TRANSFER);
				try (Socket socket = new Socket(NetworkSettings.SERVER_IP,
						NetworkSettings.SERVERPORT_OBJECT_TRANSFER)) {
					if (!socket.isConnected()) {
						socket.close();
						throw new RuntimeException("Socket is not established");
					}

					// Create the streams
					oos = new ObjectOutputStream(socket.getOutputStream());
					ois = new ObjectInputStream(socket.getInputStream());

					// Send the requests to the server
					Log.i(SharedClassesSettings.TAG, String.format("Sending %d events to server...",
							request.size()));
					for (IClientRequest icr : request)
						oos.writeObject(icr);
					oos.flush();

					// Wait for all objects to be acknowledged before closing
					// the connection
					Log.i(SharedClassesSettings.TAG, "Waiting for server confirmation ("
							+ Thread.currentThread().getId() + ")...");
					for (int i = 0; i < request.size(); i++) {
						ois.readObject();
//							Log.i(SharedClassesSettings.TAG, String.format("Received %d/%d confirmation responses", i+1, request.size()));
					}

					// Tell the server that we're ready to close the connection
					Log.i(SharedClassesSettings.TAG, "All objects confirmed, closing connection...");
					oos.writeObject(new CloseConnectionRequest());
					oos.flush();
					socket.shutdownOutput();

					// Make sure that the server isn't already dead as a doornail
					ois.mark(1);
					if (ois.read() != -1) {
						ois.reset();

						// Wait for the server to acknowledge that it's going away
						Log.i(SharedClassesSettings.TAG, "Waiting for server shutdown confirmation...");
						ois.readObject();
						Log.i(SharedClassesSettings.TAG, "Confirmation received.");
						// We close the socket anyway
						// if (socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown())
						// socket.shutdownInput();

						Log.i(SharedClassesSettings.TAG, "OK, request handling done");
					}
					Log.i(SharedClassesSettings.TAG, "Connection closed.");
				}
			}
			catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}

			if (syncToken != null && waitForResponse) {
				synchronized (syncToken) {
					syncToken.notify();
				}
			}

			Log.i(SharedClassesSettings.TAG, "End of SEND thread (" + Thread.currentThread().getId() + ").");
		}

	}
}
