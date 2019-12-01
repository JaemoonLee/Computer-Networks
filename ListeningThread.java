import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ListeningThread implements Runnable {
	
	private ServerSocket SocketListening;		// 클라이언트의 연결 요청을 기다리면서 연결 수락을 담당
	private String peerID;
	Socket remoteSocket;		// 연결된 클라이언트와 통신을 담당
	Thread sendingThread;
	
	public ListeningThread(ServerSocket socket, String peerID) {
		this.SocketListening = socket;
		this.peerID = peerID;
	}
	
	@Override
	public void run() {
		while(true)	{
			try	{
				remoteSocket = SocketListening.accept();
				sendingThread = new Thread(new RemotePeerHandler(remoteSocket,0,peerID));			// for handling individual remote peer 즉 접속하는 피어만큼 샌딩 스레드 생성
				peerProcess.showLog(peerID + " Connection is established");
				peerProcess.sendingThread.add(sendingThread);
				sendingThread.start(); 
			} catch(Exception e) {
				peerProcess.showLog(this.peerID + " Exception in connection: " + e.toString());
			}
		}
	}
	
	public void releaseSocket()	{		// 소켓 통신 마무리
		try {
			if(!remoteSocket.isClosed())
				remoteSocket.close();
		} catch (IOException e)	{
			e.printStackTrace();
		}
	}
}